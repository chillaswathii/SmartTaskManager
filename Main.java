import java.util.Scanner;
public class Main {

    public static void main(String[] args) {

        System.out.println("Smart Task Management System");
        DBConnection.getConnection();
        Scanner sc = new Scanner(System.in);
         TaskManager manager = new TaskManager();

        while (true) {

    System.out.println("\n===== TASK MANAGER =====");
    System.out.println("1. Add Task");
    System.out.println("2. Display Tasks");
    System.out.println("3. Execute Highest Priority Task");
    System.out.println("4. Delete Task");
    System.out.println("5. Search Task");
    System.out.println("6. Complete Task");
    System.out.println("7. Show Analytics");
    System.out.println("8. Exit");

    System.out.print("Enter your choice: ");
    int choice = sc.nextInt();
    switch (choice) {


     case 1:

    System.out.print("Enter Task ID: ");
    int id = sc.nextInt();

    sc.nextLine();

    System.out.print("Enter Task Name: ");
    String name = sc.nextLine();

    System.out.print("Enter Priority: ");
    int priority = sc.nextInt();

    Task t = new Task(id, name, priority);

    manager.addTask(t);

    
        break;

    case 2:

    manager.displayTasks();

    break;

    case 3:

    manager.executeTask();

    break;

    case 4:

    System.out.print("Enter Task ID to delete: ");

    int deleteId = sc.nextInt();

    manager.deleteTask(deleteId);

    break;

    case 5:

    System.out.print("Enter Task ID to search: ");

    int searchId = sc.nextInt();

    manager.searchTask(searchId);

    break;
    case 6:

    System.out.print("Enter Task ID to complete: ");

    int completeId = sc.nextInt();

    manager.completeTask(completeId);

    break;

    case 7:

    manager.showAnalytics();

    break;

    case 8:

        System.out.println("Exiting...");
        return;

    default:

        System.out.println("Invalid Choice!");

}

}
       
    }
}