package main.java.com.complaint.handler;

import main.java.com.complaint.database.DAO;
import main.java.com.complaint.model.AdminSession;
import main.java.com.complaint.model.AdminUser;
import main.java.com.complaint.model.Complaint;
import main.java.com.complaint.model.Student;
import main.java.com.complaint.queue.ComplaintQueueManager;
import main.java.com.complaint.util.SimpleJSON;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler implements Runnable {
    private final Socket socket;
    // FIX: Use ConcurrentHashMap instead of HashMap for thread safety
    private static final Map<String, Integer> activeSessions = new ConcurrentHashMap<>();

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) return;

            String method = requestParts[0];
            String path = requestParts[1];

            // Read headers
            int contentLength = 0;
            String sessionId = null;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(line.split(":", 2)[1].trim());
                    } catch (NumberFormatException e) {
                        contentLength = 0;
                    }
                } else if (line.toLowerCase().startsWith("authorization:")) {
                    String authHeader = line.split(":", 2)[1].trim();
                    if (authHeader.startsWith("Bearer ")) {
                        sessionId = authHeader.substring(7).trim();
                    }
                }
            }

            // Read body — FIX: Guard against negative or excessively large content length
            StringBuilder body = new StringBuilder();
            if (contentLength > 0 && contentLength < 1_000_000) {
                char[] buffer = new char[contentLength];
                int bytesRead = in.read(buffer, 0, contentLength);
                if (bytesRead > 0) {
                    body.append(buffer, 0, bytesRead);
                }
            }

            String response = handleRequest(method, path, body.toString(), sessionId);
            sendResponse(out, response);

        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private String handleRequest(String method, String path, String body, String sessionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            if (method.equals("OPTIONS")) {
                response.put("status", "ok");
                return SimpleJSON.toJSON(response);
            }
            switch (method) {
                case "GET":    handleGetRequest(path, response, sessionId); break;
                case "POST":   handlePostRequest(path, body, response, sessionId); break;
                case "PUT":    handlePutRequest(path, body, response, sessionId); break;
                case "DELETE": handleDeleteRequest(path, response, sessionId); break;
                default:
                    response.put("success", false);
                    response.put("message", "Method not allowed");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
            System.err.println("Request error: " + e.getMessage());
        }
        return SimpleJSON.toJSON(response);
    }

    private void handleGetRequest(String path, Map<String, Object> response, String sessionId) throws SQLException {
        if (path.equals("/api/auth/validate")) {
            if (sessionId == null || sessionId.isEmpty()) {
                response.put("success", false);
                response.put("message", "No session provided");
                return;
            }
            try {
                AdminUser admin = DAO.validateSession(sessionId);
                if (admin != null) {
                    response.put("success", true);
                    response.put("message", "Session valid");
                } else {
                    response.put("success", false);
                    response.put("message", "Invalid or expired session");
                }
            } catch (SQLException e) {
                response.put("success", false);
                response.put("message", "Error validating session");
            }
            return;
        }

        if (path.equals("/api/complaints/next")) {
            Complaint next = ComplaintQueueManager.getNextComplaint();
            if (next != null) {
                response.put("success", true);
                response.put("complaint", SimpleJSON.parse(next.toJSON()));
            } else {
                response.put("success", false);
                response.put("message", "No complaints in queue");
            }
            return;
        }

        if (path.equals("/api/complaints/all")) {
            List<Complaint> complaints = DAO.getAllComplaints();
            List<Map<String, Object>> complaintList = new ArrayList<>();
            for (Complaint c : complaints) complaintList.add(SimpleJSON.parse(c.toJSON()));
            response.put("success", true);
            response.put("complaints", complaintList);
            response.put("count", complaintList.size());
            return;
        }

        if (path.equals("/api/complaints/status")) {
            Map<String, Object> status = new HashMap<>();
            status.put("priorityQueueSize", ComplaintQueueManager.getPriorityQueueSize());
            status.put("regularQueueSize", ComplaintQueueManager.getRegularQueueSize());
            status.put("totalPending", ComplaintQueueManager.getTotalPending());
            response.put("success", true);
            response.put("status", status);
            return;
        }

        if (path.startsWith("/api/complaints/student/")) {
            // FIX: URL-decode the email in case of encoded @ signs
            String email = path.substring("/api/complaints/student/".length());
            try { email = java.net.URLDecoder.decode(email, "UTF-8"); } catch (Exception ignored) {}
            List<Complaint> complaints = DAO.getComplaintsByEmail(email);
            List<Map<String, Object>> complaintList = new ArrayList<>();
            for (Complaint c : complaints) complaintList.add(SimpleJSON.parse(c.toJSON()));
            response.put("success", true);
            response.put("complaints", complaintList);
            response.put("count", complaintList.size());
            return;
        }

        // FIX: Moved specific /status check before numeric ID check to avoid path conflict
        if (path.matches("/api/complaints/\\d+")) {
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                try {
                    int complaintId = Integer.parseInt(parts[3]);
                    Complaint complaint = DAO.getComplaintById(complaintId);
                    if (complaint != null) {
                        response.put("success", true);
                        response.put("complaint", SimpleJSON.parse(complaint.toJSON()));
                    } else {
                        response.put("success", false);
                        response.put("message", "Complaint not found");
                    }
                } catch (NumberFormatException e) {
                    response.put("success", false);
                    response.put("message", "Invalid complaint ID");
                }
            }
            return;
        }

        if (path.equals("/api/stats")) {
            Map<String, Object> stats = DAO.getStats();
            response.put("success", true);
            response.put("stats", stats);
            return;
        }

        // Health check
        if (path.equals("/") || path.equals("/health")) {
            response.put("success", true);
            response.put("status", "running");
            response.put("message", "Complaint Management API is running");
            return;
        }

        response.put("success", false);
        response.put("message", "Endpoint not found: " + path);
    }

    private void handlePostRequest(String path, String body, Map<String, Object> response, String sessionId) throws SQLException {
        if (path.equals("/api/auth/login")) {
            // FIX: Validate body is not empty before parsing
            if (body == null || body.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return;
            }
            try {
                Map<String, Object> request = SimpleJSON.parse(body);
                String username = (String) request.get("username");
                String password = (String) request.get("password");

                // FIX: Validate inputs before attempting auth
                if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Username and password are required");
                    return;
                }

                AdminUser admin = DAO.authenticateAdmin(username.trim(), password);
                if (admin != null) {
                    AdminSession session = DAO.createAdminSession(admin.getAdminId(), socket.getInetAddress().getHostAddress());
                    activeSessions.put(session.getSessionId(), admin.getAdminId());

                    response.put("success", true);
                    response.put("message", "Login successful");
                    response.put("sessionId", session.getSessionId());

                    Map<String, Object> adminData = new HashMap<>();
                    adminData.put("adminId", admin.getAdminId());
                    adminData.put("username", admin.getUsername());
                    adminData.put("fullName", admin.getFullName());
                    adminData.put("email", admin.getEmail());
                    adminData.put("role", admin.getRole());
                    response.put("admin", adminData);
                } else {
                    response.put("success", false);
                    response.put("message", "Invalid username or password");
                }
            } catch (Exception e) {
                System.err.println("Login error: " + e.getMessage());
                response.put("success", false);
                response.put("message", "Authentication error. Please try again.");
            }
            return;
        }

        if (path.equals("/api/auth/logout")) {
            if (sessionId != null && !sessionId.isEmpty()) {
                activeSessions.remove(sessionId);
                try { DAO.logoutAdmin(sessionId); } catch (SQLException e) { /* log but don't fail */ }
            }
            response.put("success", true);
            response.put("message", "Logged out successfully");
            return;
        }

        if (path.equals("/api/complaints/submit")) {
            if (body == null || body.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return;
            }
            try {
                Map<String, Object> request = SimpleJSON.parse(body);
                if (!validateComplaintRequest(request, response)) return;

                Student student = DAO.getStudentByEmail((String) request.get("email"));
                if (student == null) {
                    student = new Student(0,
                        (String) request.get("studentName"),
                        (String) request.get("email"),
                        (String) request.get("department"),
                        ((Number) request.get("semester")).intValue());
                    student = DAO.createStudent(student);
                }

                String priority = (String) request.getOrDefault("priority", "MEDIUM");
                // FIX: Validate priority value
                if (!priority.equals("LOW") && !priority.equals("MEDIUM") && !priority.equals("HIGH")) {
                    priority = "MEDIUM";
                }

                Complaint complaint = new Complaint(0, student.getId(),
                    (String) request.get("title"),
                    (String) request.get("description"),
                    (String) request.get("category"),
                    priority);

                complaint = DAO.createComplaint(complaint);
                ComplaintQueueManager.addComplaint(complaint);

                response.put("success", true);
                response.put("message", "Complaint submitted successfully");
                response.put("complaintId", complaint.getId());
                response.put("complaint", SimpleJSON.parse(complaint.toJSON()));
            } catch (Exception e) {
                System.err.println("Submit complaint error: " + e.getMessage());
                response.put("success", false);
                response.put("message", "Error submitting complaint. Please try again.");
            }
            return;
        }

        response.put("success", false);
        response.put("message", "Endpoint not found");
    }

    private void handlePutRequest(String path, String body, Map<String, Object> response, String sessionId) throws SQLException {
        if (path.matches("/api/complaints/\\d+/resolve")) {
            String[] parts = path.split("/");
            try {
                int complaintId = Integer.parseInt(parts[3]);
                if (DAO.resolveComplaint(complaintId)) {
                    ComplaintQueueManager.removeComplaint(complaintId);
                    response.put("success", true);
                    response.put("message", "Complaint resolved successfully");
                } else {
                    response.put("success", false);
                    response.put("message", "Complaint not found or already resolved");
                }
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("message", "Invalid complaint ID");
            }
            return;
        }

        if (path.matches("/api/complaints/\\d+/status")) {
            if (body == null || body.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Request body required");
                return;
            }
            try {
                Map<String, Object> request = SimpleJSON.parse(body);
                String[] parts = path.split("/");
                int complaintId = Integer.parseInt(parts[3]);
                String newStatus = (String) request.get("status");

                // FIX: Validate the status value
                List<String> validStatuses = Arrays.asList("PENDING", "IN_PROGRESS", "RESOLVED", "CLOSED");
                if (newStatus == null || !validStatuses.contains(newStatus)) {
                    response.put("success", false);
                    response.put("message", "Invalid status value");
                    return;
                }

                if (DAO.updateComplaintStatus(complaintId, newStatus)) {
                    if (newStatus.equals("RESOLVED") || newStatus.equals("CLOSED")) {
                        ComplaintQueueManager.removeComplaint(complaintId);
                    }
                    response.put("success", true);
                    response.put("message", "Status updated to " + newStatus);
                } else {
                    response.put("success", false);
                    response.put("message", "Failed to update status");
                }
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("message", "Invalid complaint ID");
            }
            return;
        }

        response.put("success", false);
        response.put("message", "Endpoint not found");
    }

    // FIX: Add DELETE handler (was completely missing)
    private void handleDeleteRequest(String path, Map<String, Object> response, String sessionId) throws SQLException {
        if (path.matches("/api/complaints/\\d+")) {
            // FIX: Require admin session for delete operations
            if (sessionId == null || sessionId.isEmpty()) {
                response.put("success", false);
                response.put("message", "Authentication required");
                return;
            }
            try {
                AdminUser admin = DAO.validateSession(sessionId);
                if (admin == null) {
                    response.put("success", false);
                    response.put("message", "Invalid or expired session");
                    return;
                }
                String[] parts = path.split("/");
                int complaintId = Integer.parseInt(parts[3]);
                if (DAO.deleteComplaint(complaintId)) {
                    ComplaintQueueManager.removeComplaint(complaintId);
                    response.put("success", true);
                    response.put("message", "Complaint deleted successfully");
                } else {
                    response.put("success", false);
                    response.put("message", "Complaint not found");
                }
            } catch (NumberFormatException e) {
                response.put("success", false);
                response.put("message", "Invalid complaint ID");
            }
            return;
        }
        response.put("success", false);
        response.put("message", "Endpoint not found");
    }

    private boolean validateComplaintRequest(Map<String, Object> request, Map<String, Object> response) {
        String[] requiredFields = {"studentName", "email", "department", "semester", "title", "description", "category"};
        for (String field : requiredFields) {
            if (!request.containsKey(field) || request.get(field) == null) {
                response.put("success", false);
                response.put("message", "Missing required field: " + field);
                return false;
            }
            Object value = request.get(field);
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Field cannot be empty: " + field);
                return false;
            }
        }
        // FIX: Stronger email validation
        String email = (String) request.get("email");
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            response.put("success", false);
            response.put("message", "Invalid email format");
            return false;
        }
        // FIX: Validate semester range
        try {
            int semester = ((Number) request.get("semester")).intValue();
            if (semester < 1 || semester > 8) {
                response.put("success", false);
                response.put("message", "Semester must be between 1 and 8");
                return false;
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid semester value");
            return false;
        }
        return true;
    }

    private void sendResponse(PrintWriter out, String response) {
        byte[] responseBytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json; charset=utf-8");
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
        out.println("Access-Control-Max-Age: 3600");
        out.println("Connection: close");
        out.println("Content-Length: " + responseBytes.length);
        out.println();
        out.println(response);
        out.flush();
    }
}
