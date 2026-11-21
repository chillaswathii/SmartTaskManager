import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Web layer for SmartTaskManager. Uses only the JDK (no extra libraries).
 * Serves the frontend from ./frontend and the JSON API under /api.
 *
 *   GET    /api/tasks              all tasks
 *   POST   /api/tasks              add task   (form fields: name, priority)
 *   POST   /api/tasks/{id}/complete
 *   DELETE /api/tasks/{id}
 *   GET    /api/analytics          total / completed / pending
 *   GET    /api/next               highest-priority pending task (or null)
 */
public class ApiServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/api", ApiServer::api);
        server.createContext("/", ApiServer::files);
        server.start();
        System.out.println("Smart Task Manager running at http://localhost:8081");
    }

    // ---------- helpers ----------

    static void send(HttpExchange x, int code, String type, byte[] body) throws IOException {
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(code, body.length);
        try (OutputStream o = x.getResponseBody()) {
            o.write(body);
        }
    }

    static void json(HttpExchange x, int code, String body) throws IOException {
        send(x, code, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    static String error(String message) {
        return "{\"error\":\"" + esc(message) + "\"}";
    }

    static String taskJson(ResultSet rs) throws SQLException {
        return "{\"id\":" + rs.getInt("id")
                + ",\"name\":\"" + esc(rs.getString("name")) + "\""
                + ",\"priority\":" + rs.getInt("priority")
                + ",\"status\":\"" + esc(rs.getString("status")) + "\"}";
    }

    static Map<String, String> formBody(HttpExchange x) throws IOException {
        String body = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return map;
    }

    // ---------- static files ----------

    static void files(HttpExchange x) throws IOException {
        String p = x.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        Path f = Paths.get("frontend", p.substring(1)).normalize();
        if (!f.startsWith("frontend") || !Files.isRegularFile(f)) {
            json(x, 404, error("Not found"));
            return;
        }
        String type = p.endsWith(".html") ? "text/html; charset=utf-8"
                : p.endsWith(".css") ? "text/css; charset=utf-8"
                : p.endsWith(".js") ? "text/javascript; charset=utf-8"
                : "application/octet-stream";
        send(x, 200, type, Files.readAllBytes(f));
    }

    // ---------- API ----------

    static void api(HttpExchange x) throws IOException {
        // DBConnection.getConnection() opens a new connection each call, so close it per request.
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                json(x, 500, error("Cannot connect to MySQL. Check that it is running and the password in DBConnection.java is correct."));
                return;
            }
            String method = x.getRequestMethod();
            String[] p = x.getRequestURI().getPath().split("/"); // ["", "api", "tasks", "5", "complete"]
            String resource = p.length > 2 ? p[2] : "";

            if (resource.equals("tasks") && method.equals("GET") && p.length == 3) {
                listTasks(x, con);
            } else if (resource.equals("tasks") && method.equals("POST") && p.length == 3) {
                addTask(x, con);
            } else if (resource.equals("tasks") && method.equals("POST") && p.length == 5 && p[4].equals("complete")) {
                completeTask(x, con, Integer.parseInt(p[3]));
            } else if (resource.equals("tasks") && method.equals("DELETE") && p.length == 4) {
                deleteTask(x, con, Integer.parseInt(p[3]));
            } else if (resource.equals("analytics") && method.equals("GET")) {
                analytics(x, con);
            } else if (resource.equals("next") && method.equals("GET")) {
                nextTask(x, con);
            } else {
                json(x, 404, error("Unknown endpoint"));
            }
        } catch (NumberFormatException e) {
            json(x, 400, error("Invalid task id"));
        } catch (Exception e) {
            e.printStackTrace();
            json(x, 500, error("Server error: " + e.getMessage()));
        }
    }

    static void listTasks(HttpExchange x, Connection con) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, priority, status FROM tasks ORDER BY id")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append(taskJson(rs));
                first = false;
            }
        }
        json(x, 200, sb.append("]").toString());
    }

    static void addTask(HttpExchange x, Connection con) throws Exception {
        Map<String, String> f = formBody(x);
        String name = f.getOrDefault("name", "").trim();
        if (name.isEmpty()) {
            json(x, 400, error("Task name is required"));
            return;
        }
        int priority;
        try {
            priority = Integer.parseInt(f.getOrDefault("priority", "3"));
        } catch (NumberFormatException e) {
            json(x, 400, error("Priority must be a number from 1 to 5"));
            return;
        }
        if (priority < 1 || priority > 5) {
            json(x, 400, error("Priority must be a number from 1 to 5"));
            return;
        }
        int id;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM tasks")) {
            rs.next();
            id = rs.getInt(1);
        }
        try (PreparedStatement pst = con.prepareStatement("INSERT INTO tasks VALUES (?, ?, ?, ?)")) {
            pst.setInt(1, id);
            pst.setString(2, name);
            pst.setInt(3, priority);
            pst.setString(4, "Pending");
            pst.executeUpdate();
        }
        json(x, 201, "{\"id\":" + id + "}");
    }

    static void completeTask(HttpExchange x, Connection con, int id) throws Exception {
        try (PreparedStatement pst = con.prepareStatement("UPDATE tasks SET status = 'Completed' WHERE id = ?")) {
            pst.setInt(1, id);
            if (pst.executeUpdate() > 0) json(x, 200, "{\"ok\":true}");
            else json(x, 404, error("Task not found"));
        }
    }

    static void deleteTask(HttpExchange x, Connection con, int id) throws Exception {
        try (PreparedStatement pst = con.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            pst.setInt(1, id);
            if (pst.executeUpdate() > 0) json(x, 200, "{\"ok\":true}");
            else json(x, 404, error("Task not found"));
        }
    }

    static void analytics(HttpExchange x, Connection con) throws Exception {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*), COALESCE(SUM(status = 'Completed'), 0) FROM tasks")) {
            rs.next();
            int total = rs.getInt(1);
            int completed = rs.getInt(2);
            json(x, 200, "{\"total\":" + total + ",\"completed\":" + completed
                    + ",\"pending\":" + (total - completed) + "}");
        }
    }

    static void nextTask(HttpExchange x, Connection con) throws Exception {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, name, priority, status FROM tasks WHERE status <> 'Completed' "
                             + "ORDER BY priority DESC, id ASC LIMIT 1")) {
            json(x, 200, rs.next() ? taskJson(rs) : "null");
        }
    }
}
