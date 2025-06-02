import java.sql.*;
import java.util.Scanner;

public class HotelReservationDB {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system"; // Your Oracle username
    private static final String PASS = "system"; // Your Oracle password

    private static Connection conn;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            // Load Oracle JDBC Driver
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Establish connection
            conn = DriverManager.getConnection(URL, USER, PASS);

            while (true) {
                System.out.println("\n=== Hotel Reservation System ===");
                System.out.println("1. View Available Rooms");
                System.out.println("2. Make Reservation");
                System.out.println("3. Cancel Reservation");
                System.out.println("4. View All Reservations");
                System.out.println("5. Exit");
                System.out.print("Enter choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1: showAvailableRooms(); break;
                    case 2: makeReservation(); break;
                    case 3: cancelReservation(); break;
                    case 4: viewReservations(); break;
                    case 5: conn.close(); return;
                    default: System.out.println("Invalid option.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void showAvailableRooms() throws SQLException {
        String query = "SELECT * FROM Room WHERE is_booked = 0";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        System.out.println("\n--- Available Rooms ---");
        while (rs.next()) {
            System.out.printf("Room #%d [%s] - ₹%.2f\n",
                rs.getInt("room_id"),
                rs.getString("type"),
                getRoomPrice(rs.getString("type")));
        }
    }

    static void makeReservation() throws SQLException {
        showAvailableRooms();

        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Room ID to book: ");
        int roomId = scanner.nextInt();
        scanner.nextLine();

        PreparedStatement check = conn.prepareStatement("SELECT is_booked, type FROM Room WHERE room_id = ?");
        check.setInt(1, roomId);
        ResultSet rs = check.executeQuery();

        if (rs.next() && rs.getInt("is_booked") == 0) {
            // Insert reservation
            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO Reservation (reservation_id, customer_name, room_id) VALUES (reservation_seq.NEXTVAL, ?, ?)");
            insert.setString(1, name);
            insert.setInt(2, roomId);
            insert.executeUpdate();

            // Mark room as booked
            PreparedStatement update = conn.prepareStatement("UPDATE Room SET is_booked = 1 WHERE room_id = ?");
            update.setInt(1, roomId);
            update.executeUpdate();

            double price = getRoomPrice(rs.getString("type"));
            System.out.printf("Reservation Successful! Total Price: ₹%.2f\n", price);
        } else {
            System.out.println(" Room not available.");
        }
    }

    static void cancelReservation() throws SQLException {
        System.out.print("Enter Reservation ID to cancel: ");
        int resId = scanner.nextInt();
        scanner.nextLine();

        PreparedStatement find = conn.prepareStatement("SELECT room_id FROM Reservation WHERE reservation_id = ?");
        find.setInt(1, resId);
        ResultSet rs = find.executeQuery();

        if (rs.next()) {
            int roomId = rs.getInt("room_id");

            PreparedStatement delete = conn.prepareStatement("DELETE FROM Reservation WHERE reservation_id = ?");
            delete.setInt(1, resId);
            delete.executeUpdate();

            PreparedStatement update = conn.prepareStatement("UPDATE Room SET is_booked = 0 WHERE room_id = ?");
            update.setInt(1, roomId);
            update.executeUpdate();

            System.out.println(" Reservation Cancelled.");
        } else {
            System.out.println(" Reservation ID not found.");
        }
    }

    static void viewReservations() throws SQLException {
        String query = "SELECT r.reservation_id, r.customer_name, rm.room_id, rm.type FROM Reservation r JOIN Room rm ON r.room_id = rm.room_id";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        System.out.println("\n--- All Reservations ---");
        while (rs.next()) {
            System.out.printf("ID: %d | Name: %s | Room #%d [%s]\n",
                rs.getInt("reservation_id"),
                rs.getString("customer_name"),
                rs.getInt("room_id"),
                rs.getString("type"));
        }
    }

    static double getRoomPrice(String type) {
        switch (type.toUpperCase()) {
            case "STANDARD": return 1000;
            case "DELUXE": return 2000;
            case "SUITE": return 3500;
            default: return 0;
        }
    }
}
