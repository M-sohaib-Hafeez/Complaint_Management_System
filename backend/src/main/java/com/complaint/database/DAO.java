package main.java.com.complaint.database;

import main.java.com.complaint.model.Complaint;
import main.java.com.complaint.model.Student;
import main.java.com.complaint.model.AdminUser;           // ADD THIS
import main.java.com.complaint.model.AdminSession;        // ADD THIS
import main.java.com.complaint.util.PasswordUtil;         // ADD THIS
import java.sql.*;
import java.time.LocalDateTime;                 // ADD THIS
import java.time.format.DateTimeFormatter;      // ADD THIS
import java.util.*;

public class DAO {

    private static void ensureConnection() throws SQLException {
        if (!DatabaseConnection.isConnected()) {
            System.out.println("⚠️ Database connection lost, reconnecting...");
            DatabaseConnection.initialize();
        }
    }

    // =============== STUDENT METHODS ===============

    public static Student getStudentByEmail(String email) throws SQLException {
        ensureConnection();

        String sql = "SELECT s.*, d.department_name FROM students s " +
                "LEFT JOIN departments d ON s.department_id = d.department_id " +
                "WHERE s.email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Student(
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("email"),
                        rs.getString("department_name"),
                        rs.getInt("semester")
                );
            }
            return null;
        }
    }

    public static Student getStudentById(int studentId) throws SQLException {
        ensureConnection();

        String sql = "SELECT s.*, d.department_name FROM students s " +
                "LEFT JOIN departments d ON s.department_id = d.department_id " +
                "WHERE s.student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Student(
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("email"),
                        rs.getString("department_name"),
                        rs.getInt("semester")
                );
            }
            return null;
        }
    }

    public static Student createStudent(Student student) throws SQLException {
        ensureConnection();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First get or create department
            int deptId = getOrCreateDepartment(conn, student.getDepartment());

            // Create student
            String sql = "INSERT INTO students (student_name, email, department_id, semester) " +
                    "VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, student.getName());
                pstmt.setString(2, student.getEmail());
                pstmt.setInt(3, deptId);
                pstmt.setInt(4, student.getSemester());
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    conn.commit();
                    System.out.println("✅ Created student with ID: " + newId);

                    return new Student(
                            newId,
                            student.getName(),
                            student.getEmail(),
                            student.getDepartment(),
                            student.getSemester()
                    );
                }
            }

            conn.rollback();
            return student;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<Student> getAllStudents() throws SQLException {
        ensureConnection();

        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.*, d.department_name FROM students s " +
                "LEFT JOIN departments d ON s.department_id = d.department_id " +
                "ORDER BY s.student_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("email"),
                        rs.getString("department_name"),
                        rs.getInt("semester")
                ));
            }
        }
        return students;
    }

    public static boolean updateStudent(Student student) throws SQLException {
        ensureConnection();

        String sql = "UPDATE students SET student_name = ?, email = ?, semester = ? " +
                "WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setInt(3, student.getSemester());
            pstmt.setInt(4, student.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean deleteStudent(int studentId) throws SQLException {
        ensureConnection();

        String sql = "DELETE FROM students WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // =============== COMPLAINT METHODS ===============

    public static Complaint createComplaint(Complaint complaint) throws SQLException {
        ensureConnection();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Get or create category
            int catId = getOrCreateCategory(conn, complaint.getCategory());

            // Create complaint
            String sql = "INSERT INTO complaints (student_id, category_id, title, description, priority, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, complaint.getStudentId());
                pstmt.setInt(2, catId);
                pstmt.setString(3, complaint.getTitle());
                pstmt.setString(4, complaint.getDescription());
                pstmt.setString(5, complaint.getPriority());
                pstmt.setString(6, complaint.getStatus());
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    conn.commit();
                    System.out.println("✅ Created complaint with ID: " + newId);

                    return new Complaint(
                            newId,
                            complaint.getStudentId(),
                            complaint.getTitle(),
                            complaint.getDescription(),
                            complaint.getCategory(),
                            complaint.getPriority(),
                            complaint.getStatus(),
                            complaint.getSubmissionDate(),
                            complaint.getResolutionDate()
                    );
                }
            }

            conn.rollback();
            return complaint;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public static Complaint getComplaintById(int complaintId) throws SQLException {
        ensureConnection();

        String sql = "SELECT c.*, cat.category_name FROM complaints c " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "WHERE c.complaint_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                );
            }
            return null;
        }
    }

    public static List<Complaint> getAllComplaints() throws SQLException {
        ensureConnection();

        List<Complaint> complaints = new ArrayList<>();
        String sql = "SELECT c.*, cat.category_name FROM complaints c " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "ORDER BY " +
                "CASE c.priority " +
                "  WHEN 'HIGH' THEN 1 " +
                "  WHEN 'MEDIUM' THEN 2 " +
                "  WHEN 'LOW' THEN 3 " +
                "END, c.submission_date DESC"; // Sort by priority then submission date

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Complaint complaint = new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                );
                complaints.add(complaint);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all complaints: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return complaints;
    }

    public static List<Complaint> getComplaintsByEmail(String email) throws SQLException {
        ensureConnection();

        List<Complaint> complaints = new ArrayList<>();
        String sql = "SELECT c.*, cat.category_name FROM complaints c " +
                "JOIN students s ON c.student_id = s.student_id " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "WHERE s.email = ? " +
                "ORDER BY " +
                "CASE c.priority " +
                "  WHEN 'HIGH' THEN 1 " +
                "  WHEN 'MEDIUM' THEN 2 " +
                "  WHEN 'LOW' THEN 3 " +
                "END, c.submission_date DESC"; // Sort by priority then submission date

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                complaints.add(new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                ));
            }
        }
        return complaints;
    }

    public static List<Complaint> getComplaintsByStudentId(int studentId) throws SQLException {
        ensureConnection();

        List<Complaint> complaints = new ArrayList<>();
        String sql = "SELECT c.*, cat.category_name FROM complaints c " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "WHERE c.student_id = ? " +
                "ORDER BY " +
                "CASE c.priority " +
                "  WHEN 'HIGH' THEN 1 " +
                "  WHEN 'MEDIUM' THEN 2 " +
                "  WHEN 'LOW' THEN 3 " +
                "END, c.submission_date DESC"; // Sort by priority then submission date

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                complaints.add(new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                ));
            }
        }
        return complaints;
    }

    public static List<Complaint> getPendingComplaints() throws SQLException {
        ensureConnection();

        List<Complaint> complaints = new ArrayList<>();
        String sql = "SELECT c.*, cat.category_name FROM complaints c " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "WHERE c.status = 'PENDING' " +
                "ORDER BY " +
                "CASE c.priority " +
                "  WHEN 'HIGH' THEN 1 " +
                "  WHEN 'MEDIUM' THEN 2 " +
                "  WHEN 'LOW' THEN 3 " +
                "END, c.submission_date ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Complaint complaint = new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                );
                complaints.add(complaint);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all complaints: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return complaints;
    }

    public static Complaint getNextComplaint() throws SQLException {
        ensureConnection();

        String sql = "SELECT c.*, cat.category_name FROM complaints c " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "WHERE c.status = 'PENDING' " +
                "ORDER BY " +
                "CASE c.priority " +
                "  WHEN 'HIGH' THEN 1 " +
                "  WHEN 'MEDIUM' THEN 2 " +
                "  WHEN 'LOW' THEN 3 " +
                "END, c.submission_date ASC " +
                "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        "PENDING",
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                );
            }
            return null;
        }
    }

    public static boolean resolveComplaint(int complaintId) throws SQLException {
        ensureConnection();

        String sql = "UPDATE complaints SET status = 'RESOLVED', resolution_date = NOW() " +
                "WHERE complaint_id = ? AND status != 'RESOLVED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean updateComplaintStatus(int complaintId, String newStatus) throws SQLException {
        ensureConnection();

        String sql = "UPDATE complaints SET status = ?, resolution_date = CASE WHEN ? = 'RESOLVED' THEN NOW() ELSE NULL END WHERE complaint_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, newStatus); // For the CASE statement
            pstmt.setInt(3, complaintId);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static boolean updateComplaint(Complaint complaint) throws SQLException {
        ensureConnection();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Get or create category
            int catId = getOrCreateCategory(conn, complaint.getCategory());

            // Update complaint
            String sql = "UPDATE complaints SET title = ?, description = ?, category_id = ?, " +
                    "priority = ?, status = ?, resolution_date = ? " +
                    "WHERE complaint_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, complaint.getTitle());
                pstmt.setString(2, complaint.getDescription());
                pstmt.setInt(3, catId);
                pstmt.setString(4, complaint.getPriority());
                pstmt.setString(5, complaint.getStatus());

                if (complaint.getResolutionDate() != null && !complaint.getResolutionDate().isEmpty()) {
                    pstmt.setString(6, complaint.getResolutionDate());
                } else {
                    pstmt.setNull(6, Types.TIMESTAMP);
                }

                pstmt.setInt(7, complaint.getId());

                boolean updated = pstmt.executeUpdate() > 0;
                conn.commit();
                return updated;
            }

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public static boolean deleteComplaint(int complaintId) throws SQLException {
        ensureConnection();

        String sql = "DELETE FROM complaints WHERE complaint_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<Complaint> searchComplaints(String keyword, String category, String status, String priority) throws SQLException {
        ensureConnection();

        List<Complaint> complaints = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT c.*, cat.category_name FROM complaints c " +
                        "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                        "LEFT JOIN students s ON c.student_id = s.student_id " +
                        "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (c.title LIKE ? OR c.description LIKE ? OR s.student_name LIKE ? OR s.email LIKE ?)");
            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (category != null && !category.trim().isEmpty()) {
            sql.append(" AND cat.category_name = ?");
            params.add(category);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND c.status = ?");
            params.add(status);
        }

        if (priority != null && !priority.trim().isEmpty()) {
            sql.append(" AND c.priority = ?");
            params.add(priority);
        }

        sql.append(" ORDER BY " +
                "CASE c.priority " +
                "  WHEN 'HIGH' THEN 1 " +
                "  WHEN 'MEDIUM' THEN 2 " +
                "  WHEN 'LOW' THEN 3 " +
                "END, c.submission_date DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                complaints.add(new Complaint(
                        rs.getInt("complaint_id"),
                        rs.getInt("student_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("category_name"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("submission_date"),
                        rs.getString("resolution_date")
                ));
            }
        }
        return complaints;
    }



    // =============== STATISTICS METHODS ===============

    public static Map<String, Object> getStats() throws SQLException {
        ensureConnection();

        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending, " +
                "SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress, " +
                "SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved, " +
                "COUNT(DISTINCT student_id) as students, " +
                "SUM(CASE WHEN priority = 'HIGH' THEN 1 ELSE 0 END) as 'high_priority' " +
                "FROM complaints";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("total", rs.getInt("total"));
                stats.put("pending", rs.getInt("pending"));
                stats.put("inProgress", rs.getInt("in_progress"));
                stats.put("resolved", rs.getInt("resolved"));
                stats.put("students", rs.getInt("students"));
                stats.put("highPriority", rs.getInt("high_priority"));
                return stats;
            }
            return new HashMap<>();
        }
    }

    public static Map<String, Object> getCategoryStats() throws SQLException {
        ensureConnection();

        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT cat.category_name, COUNT(c.complaint_id) as count " +
                "FROM categories cat " +
                "LEFT JOIN complaints c ON cat.category_id = c.category_id " +
                "GROUP BY cat.category_id, cat.category_name " +
                "ORDER BY count DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stats.put(rs.getString("category_name"), rs.getInt("count"));
            }
        }
        return stats;
    }

    public static Map<String, Object> getPriorityStats() throws SQLException {
        ensureConnection();

        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT priority, COUNT(*) as count FROM complaints GROUP BY priority";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stats.put(rs.getString("priority"), rs.getInt("count"));
            }
        }
        return stats;
    }

    public static Map<String, Object> getMonthlyStats() throws SQLException {
        ensureConnection();

        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT DATE_FORMAT(submission_date, '%Y-%m') as month, " +
                "COUNT(*) as count, " +
                "SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved " +
                "FROM complaints " +
                "GROUP BY DATE_FORMAT(submission_date, '%Y-%m') " +
                "ORDER BY month DESC " +
                "LIMIT 6";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> monthStats = new HashMap<>();
                monthStats.put("total", rs.getInt("count"));
                monthStats.put("resolved", rs.getInt("resolved"));
                stats.put(rs.getString("month"), monthStats);
            }
        }
        return stats;
    }

    // =============== HELPER METHODS ===============

    private static int getOrCreateDepartment(Connection conn, String deptName) throws SQLException {
        // Try to get existing department
        String selectSql = "SELECT department_id FROM departments WHERE department_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, deptName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("department_id");
            }
        }

        // Create new department
        String insertSql = "INSERT INTO departments (department_name, department_code) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, deptName);
            pstmt.setString(2, deptName.substring(0, Math.min(2, deptName.length())).toUpperCase());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to create department");
        }
    }

    private static int getOrCreateCategory(Connection conn, String categoryName) throws SQLException {
        // Try to get existing category
        String selectSql = "SELECT category_id FROM categories WHERE category_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("category_id");
            }
        }

        // Create new category
        String insertSql = "INSERT INTO categories (category_name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoryName);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to create category");
        }
    }

    public static Map<String, Object> checkDatabaseHealth() throws SQLException {
        Map<String, Object> health = new HashMap<>();

        try {
            ensureConnection();

            if (DatabaseConnection.isConnected()) {
                health.put("database", "connected");
                health.put("timestamp", System.currentTimeMillis());

                // Get table counts
                String[] tables = {"students", "complaints", "departments", "categories"};
                Map<String, Integer> tableCounts = new HashMap<>();

                for (String table : tables) {
                    try {
                        String sql = "SELECT COUNT(*) as count FROM " + table;
                        try (Connection conn = DatabaseConnection.getConnection();
                             Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(sql)) {

                            if (rs.next()) {
                                tableCounts.put(table, rs.getInt("count"));
                            }
                        }
                    } catch (SQLException e) {
                        tableCounts.put(table, -1); // Table doesn't exist
                    }
                }

                health.put("table_counts", tableCounts);

            } else {
                health.put("database", "disconnected");
                health.put("error", "Cannot connect to database");
            }

        } catch (Exception e) {
            health.put("database", "error");
            health.put("error", e.getMessage());
        }

        return health;
    }

    public static List<Map<String, Object>> getRecentActivity(int limit) throws SQLException {
        ensureConnection();

        List<Map<String, Object>> activity = new ArrayList<>();
        String sql = "SELECT c.*, cat.category_name, s.student_name, s.email " +
                "FROM complaints c " +
                "LEFT JOIN categories cat ON c.category_id = cat.category_id " +
                "LEFT JOIN students s ON c.student_id = s.student_id " +
                "ORDER BY c.last_updated DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("complaintId", rs.getInt("complaint_id"));
                record.put("title", rs.getString("title"));
                record.put("studentName", rs.getString("student_name"));
                record.put("category", rs.getString("category_name"));
                record.put("status", rs.getString("status"));
                record.put("priority", rs.getString("priority"));
                record.put("lastUpdated", rs.getString("last_updated"));
                activity.add(record);
            }
        }
        return activity;
    }

    public static boolean addComment(int complaintId, String commenterName, String commenterType,
                                     String commentText, boolean isInternal) throws SQLException {
        ensureConnection();

        // First check if complaint exists
        Complaint complaint = getComplaintById(complaintId);
        if (complaint == null) {
            return false;
        }

        String sql = "INSERT INTO complaint_comments (complaint_id, commenter_name, commenter_type, " +
                "comment_text, is_internal) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            pstmt.setString(2, commenterName);
            pstmt.setString(3, commenterType);
            pstmt.setString(4, commentText);
            pstmt.setBoolean(5, isInternal);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<Map<String, Object>> getComments(int complaintId, boolean includeInternal) throws SQLException {
        ensureConnection();

        List<Map<String, Object>> comments = new ArrayList<>();
        String sql = "SELECT * FROM complaint_comments WHERE complaint_id = ? ";

        if (!includeInternal) {
            sql += "AND is_internal = FALSE ";
        }

        sql += "ORDER BY created_at ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> comment = new HashMap<>();
                comment.put("commentId", rs.getInt("comment_id"));
                comment.put("commenterName", rs.getString("commenter_name"));
                comment.put("commenterType", rs.getString("commenter_type"));
                comment.put("commentText", rs.getString("comment_text"));
                comment.put("isInternal", rs.getBoolean("is_internal"));
                comment.put("createdAt", rs.getString("created_at"));
                comments.add(comment);
            }
        }
        return comments;
    }

    public static boolean logStatusChange(int complaintId, String oldStatus, String newStatus,
                                          String changedBy, String changeReason) throws SQLException {
        ensureConnection();

        String sql = "INSERT INTO complaint_history (complaint_id, old_status, new_status, " +
                "changed_by, change_reason) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            pstmt.setString(2, oldStatus);
            pstmt.setString(3, newStatus);
            pstmt.setString(4, changedBy);
            pstmt.setString(5, changeReason);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static List<Map<String, Object>> getStatusHistory(int complaintId) throws SQLException {
        ensureConnection();

        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT * FROM complaint_history WHERE complaint_id = ? ORDER BY changed_at ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, complaintId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("historyId", rs.getInt("history_id"));
                record.put("oldStatus", rs.getString("old_status"));
                record.put("newStatus", rs.getString("new_status"));
                record.put("changedBy", rs.getString("changed_by"));
                record.put("changeReason", rs.getString("change_reason"));
                record.put("changedAt", rs.getString("changed_at"));
                history.add(record);
            }
        }
        return history;
    }

    // =============== ADMIN AUTHENTICATION METHODS ===============

    /**
     * Authenticate admin user
     */
    public static AdminUser authenticateAdmin(String username, String password) throws SQLException {
        ensureConnection();

        String sql = "SELECT * FROM admin_users WHERE username = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                System.out.println("🔐 Attempting to authenticate user: " + username);
                System.out.println("📝 Stored hash: " + storedHash);

                // Verify password
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    System.out.println("✅ Password verified successfully!");

                    AdminUser admin = new AdminUser(
                            rs.getInt("admin_id"),
                            rs.getString("username"),
                            storedHash,
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getBoolean("is_active"),
                            rs.getString("created_at"),
                            rs.getString("last_login")
                    );

                    // Update last login
                    updateLastLogin(conn, admin.getAdminId());
                    return admin;
                } else {
                    System.out.println("❌ Password verification failed!");
                }
            } else {
                System.out.println("❌ User not found: " + username);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("❌ SQL Error in authenticateAdmin: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Update admin last login time
     */
    private static void updateLastLogin(Connection conn, int adminId) throws SQLException {
        String sql = "UPDATE admin_users SET last_login = NOW() WHERE admin_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            pstmt.executeUpdate();
            System.out.println("✅ Updated last login for admin ID: " + adminId);
        }
    }

    /**
     * Create admin session
     */
    public static AdminSession createAdminSession(int adminId, String ipAddress) throws SQLException {
        ensureConnection();

        String sessionId = PasswordUtil.generateSessionId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(8); // 8 hour session

        String sql = "INSERT INTO admin_sessions (session_id, admin_id, expires_at, ip_address) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sessionId);
            pstmt.setInt(2, adminId);
            pstmt.setString(3, expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setString(4, ipAddress);
            pstmt.executeUpdate();

            System.out.println("✅ Created session: " + sessionId);

            return new AdminSession(
                    sessionId,
                    adminId,
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    ipAddress,
                    null
            );
        }
    }

    /**
     * Validate admin session
     */
    public static AdminUser validateSession(String sessionId) throws SQLException {
        ensureConnection();

        String sql = "SELECT a.* FROM admin_users a " +
                "JOIN admin_sessions s ON a.admin_id = s.admin_id " +
                "WHERE s.session_id = ? AND s.expires_at > NOW() AND a.is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("✅ Session valid: " + sessionId);
                return new AdminUser(
                        rs.getInt("admin_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"),
                        rs.getString("created_at"),
                        rs.getString("last_login")
                );
            } else {
                System.out.println("❌ Session invalid or expired: " + sessionId);
            }
            return null;
        }
    }

    /**
     * Logout admin (delete session)
     */
    public static boolean logoutAdmin(String sessionId) throws SQLException {
        ensureConnection();

        String sql = "DELETE FROM admin_sessions WHERE session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sessionId);
            int deleted = pstmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("✅ Logged out session: " + sessionId);
                return true;
            }
            return false;
        }
    }

    /**
     * Clean up expired sessions
     */
    public static int cleanupExpiredSessions() throws SQLException {
        ensureConnection();

        String sql = "DELETE FROM admin_sessions WHERE expires_at < NOW()";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            int deleted = stmt.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("🧹 Cleaned up " + deleted + " expired sessions");
            }
            return deleted;
        }
    }
}