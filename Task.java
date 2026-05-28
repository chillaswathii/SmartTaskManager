class Task {

    int id;
    String name;
    int priority;
    String status;
    Task(int id, String name, int priority) {

    this.id = id;
    this.name = name;
    this.priority = priority;
    this.status = "Pending";

}
public int getId() {
    return id;
}

public String getName() {
    return name;
}

public int getPriority() {
    return priority;
}

public String getStatus() {
    return status;
}
void display() {

    System.out.println("===== TASK DETAILS =====");
    System.out.println("Task ID      : " + id);
    System.out.println("Task Name    : " + name);
    System.out.println("Priority     : " + priority);
    System.out.println("Status       : " + status);
    System.out.println("========================");

}

}