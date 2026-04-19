package main.java.com.complaint;

import main.java.com.complaint.database.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class  Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("🎯 Student Complaint Management System Backend");
        System.out.println("📊 Data Structures Used: Priority Queue & Regular Queue");
        System.out.println("🌐 Starting server on port " + PORT + "...");

        // Initialize database connection
        System.out.println("\n🗄️  Initializing Database Connection...");
        DatabaseConnection.initialize();

        if (!DatabaseConnection.isConnected()) {
            System.out.println("⚠️  WARNING: Running without database connection!");
            System.out.println("   Some features may not work properly.");
            System.out.println("   Check database configuration and try again.");
        }

        // Start HTTP server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("\n✅ Server started successfully on port " + PORT + "!");
            System.out.println("📌 Available Endpoints:");
            System.out.println("  GET  /                      - Health check");
            System.out.println("  POST /api/complaints/submit - Submit new complaint");
            System.out.println("  GET  /api/complaints/next   - Get next complaint to process");
            System.out.println("  GET  /api/complaints/all    - Get all complaints");
            System.out.println("  GET  /api/complaints/status - Get queue status");
            System.out.println("  GET  /api/complaints/student/:email - Get student complaints");
            System.out.println("  PUT  /api/complaints/:id/resolve - Resolve complaint");
            System.out.println("  GET  /api/stats            - Get system statistics");
            System.out.println("\n🔗 Frontend URL: http://localhost:8000 (or file://path/to/index.html)");
            System.out.println("🔗 API URL: http://localhost:" + PORT);
            System.out.println("\n🚀 System ready! Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n🔗 New connection from: " + clientSocket.getInetAddress());

                // Use fully qualified class name to avoid ambiguity
                main.java.com.complaint.handler.RequestHandler handler = new main.java.com.complaint.handler.RequestHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
            if (e.getMessage().contains("Address already in use")) {
                System.err.println("   Port " + PORT + " is already in use.");
                System.err.println("   Change port in Main.java or kill the process using port " + PORT);
            }
        }
    }
}