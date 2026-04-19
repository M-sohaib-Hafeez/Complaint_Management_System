package main.java.com.complaint.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // ===== CHANGE THESE VALUES TO MATCH YOUR DATABASE ===== //
    private static final String DB_URL = "jdbc:mysql://localhost:3306/complaint_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true&useUnicode=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "your_password_here"; // CHANGE THIS
    // ====================================================== //

    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;
        try {
            System.out.println("Attempting database connection...");
            System.out.println("   URL: " + DB_URL);
            System.out.println("   User: " + DB_USER);
            // NOTE: Never log passwords
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection testConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                if (testConn.isValid(5)) {
                    System.out.println("Database connected successfully!");
                    initialized = true;
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found! Add MySQL Connector JAR to your project.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.err.println("Possible solutions:");
            System.err.println("  1. Check if MySQL is running");
            System.err.println("  2. Verify database 'complaint_db' exists");
            System.err.println("  3. Check username and password in DatabaseConnection.java");
            System.err.println("  4. Make sure MySQL is running on port 3306");
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) initialize();
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static boolean isConnected() {
        try (Connection testConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return testConn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public static void close() {
        System.out.println("Connection pool cleaned up");
    }
}
