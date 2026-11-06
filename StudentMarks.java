import java.util.*;

public class StudentMarks {

    static HashMap<Integer, Double> students = new HashMap<>();

    // Add student record
    static void addRecord(int roll, double percentage) {
        students.put(roll, percentage);
    }

    // Display records in sorted order
    static void displayAllSorted() {
        TreeMap<Integer, Double> sorted = new TreeMap<>(students);

        System.out.println("\nStudent Records:");
        for (int roll : sorted.keySet()) {
            System.out.println(roll + " : " + sorted.get(roll) + "%");
        }
    }

    // Search student by roll number
    static void searchByRoll(int roll) {
        if (students.containsKey(roll))
            System.out.println("Percentage: " + students.get(roll) + "%");
        else
            System.out.println("Student Not Found");
    }

    // Display topper and fail lists
    static void displayLists() {
        System.out.println("\nTopper List:");
        for (int roll : students.keySet()) {
            if (students.get(roll) >= 75)
                System.out.println(roll + " : " + students.get(roll) + "%");
        }

        System.out.println("\nFail List:");
        for (int roll : students.keySet()) {
            if (students.get(roll) < 40)
                System.out.println(roll + " : " + students.get(roll) + "%");
        }
    }

    public static void main(String[] args) {

        addRecord(101, 85.5);
        addRecord(102, 35.0);
        addRecord(103, 92.0);
        addRecord(104, 65.5);
        addRecord(105, 28.0);

        displayAllSorted();

        System.out.println("\nSearch Roll No 103:");
        searchByRoll(103);

        displayLists();
    }
}