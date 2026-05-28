import java.util.ArrayList;
import java.util.PriorityQueue;
import java.sql.*;

class TaskManager {

    ArrayList<Task> taskList = new ArrayList<>();

    PriorityQueue<Task> pq = new PriorityQueue<>(
        (a, b) -> b.priority - a.priority
    );
public void addTask(Task t) {

    try {

        Connection con = DBConnection.getConnection();

        String query = "INSERT INTO tasks VALUES (?, ?, ?, ?)";

        PreparedStatement pst = con.prepareStatement(query);

        pst.setInt(1, t.getId());
        pst.setString(2, t.getName());
        pst.setInt(3, t.getPriority());
        pst.setString(4, t.getStatus());

        pst.executeUpdate();

        System.out.println("Task added to database successfully!");

    } catch (Exception e) {

        System.out.println(e);

    }

}
   public void displayTasks() {

    try {

        Connection con = DBConnection.getConnection();

        String query = "SELECT * FROM tasks";

        Statement st = con.createStatement();

        ResultSet rs = st.executeQuery(query);

        while (rs.next()) {

            System.out.println("===== TASK DETAILS =====");

            System.out.println("Task ID      : " + rs.getInt("id"));
            System.out.println("Task Name    : " + rs.getString("name"));
            System.out.println("Priority     : " + rs.getInt("priority"));
            System.out.println("Status       : " + rs.getString("status"));

            System.out.println("========================");
        }

    } catch (Exception e) {

        System.out.println(e);

    }

}
    public void executeTask() {

    try {

        Connection con = DBConnection.getConnection();

        String query = "SELECT * FROM tasks ORDER BY priority DESC LIMIT 1";

        Statement st = con.createStatement();

        ResultSet rs = st.executeQuery(query);

        if (rs.next()) {

            System.out.println("Executing Highest Priority Task:");

            System.out.println("===== TASK DETAILS =====");

            System.out.println("Task ID      : " + rs.getInt("id"));
            System.out.println("Task Name    : " + rs.getString("name"));
            System.out.println("Priority     : " + rs.getInt("priority"));
            System.out.println("Status       : " + rs.getString("status"));

            System.out.println("========================");

        } else {

            System.out.println("No tasks available!");

        }

    } catch (Exception e) {

        System.out.println(e);

    }

}
public void deleteTask(int id) {

    try {

        Connection con = DBConnection.getConnection();

        String query = "DELETE FROM tasks WHERE id = ?";

        PreparedStatement pst = con.prepareStatement(query);

        pst.setInt(1, id);

        int rows = pst.executeUpdate();

        if (rows > 0) {

            System.out.println("Task deleted successfully!");

        } else {

            System.out.println("Task not found!");

        }

    } catch (Exception e) {

        System.out.println(e);

    }

}
public void searchTask(int id) {

    try {

        Connection con = DBConnection.getConnection();

        String query = "SELECT * FROM tasks WHERE id = ?";

        PreparedStatement pst = con.prepareStatement(query);

        pst.setInt(1, id);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {

            System.out.println("Task Found!");

            System.out.println("===== TASK DETAILS =====");

            System.out.println("Task ID      : " + rs.getInt("id"));
            System.out.println("Task Name    : " + rs.getString("name"));
            System.out.println("Priority     : " + rs.getInt("priority"));
            System.out.println("Status       : " + rs.getString("status"));

            System.out.println("========================");

        } else {

            System.out.println("Task not found!");

        }

    } catch (Exception e) {

        System.out.println(e);

    }

}
public void completeTask(int id) {

    try {

        Connection con = DBConnection.getConnection();

        String query = "UPDATE tasks SET status = 'Completed' WHERE id = ?";

        PreparedStatement pst = con.prepareStatement(query);

        pst.setInt(1, id);

        int rows = pst.executeUpdate();

        if (rows > 0) {

            System.out.println("Task marked as completed!");

        } else {

            System.out.println("Task not found!");

        }

    } catch (Exception e) {

        System.out.println(e);

    }

}
public void showAnalytics() {

    try {

        Connection con = DBConnection.getConnection();

        Statement st = con.createStatement();

        ResultSet totalRs = st.executeQuery("SELECT COUNT(*) FROM tasks");

        totalRs.next();

        int total = totalRs.getInt(1);

        ResultSet completedRs = st.executeQuery(
            "SELECT COUNT(*) FROM tasks WHERE status = 'Completed'"
        );

        completedRs.next();

        int completed = completedRs.getInt(1);

        int pending = total - completed;

        System.out.println("===== ANALYTICS =====");

        System.out.println("Total Tasks      : " + total);
        System.out.println("Completed Tasks  : " + completed);
        System.out.println("Pending Tasks    : " + pending);

        System.out.println("=====================");

    } catch (Exception e) {

        System.out.println(e);

    }

}
}