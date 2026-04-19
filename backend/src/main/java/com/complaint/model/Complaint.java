package main.java.com.complaint.model;

import main.java.com.complaint.util.SimpleJSON;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Complaint implements SimpleJSON.JSONable {
    private int id;
    private int studentId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String submissionDate;
    private String resolutionDate;

    public Complaint(int id, int studentId, String title, String description,
                     String category, String priority) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = "PENDING";
        this.submissionDate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Database constructor
    public Complaint(int id, int studentId, String title, String description,
                     String category, String priority, String status,
                     String submissionDate, String resolutionDate) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.submissionDate = submissionDate;
        this.resolutionDate = resolutionDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getSubmissionDate() { return submissionDate; }
    public String getResolutionDate() { return resolutionDate; }

    public void setStatus(String status) { this.status = status; }
    public void setResolutionDate(String date) { this.resolutionDate = date; }

    @Override
    public String toJSON() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("complaintId", id);
        map.put("studentId", studentId);
        map.put("title", title);
        map.put("description", description);
        map.put("categoryName", category); // FIX: Use categoryName to match frontend expectation
        map.put("priority", priority);
        map.put("status", status);
        map.put("submissionDate", submissionDate);
        map.put("resolutionDate", resolutionDate != null ? resolutionDate : null);
        return SimpleJSON.toJSON(map);
    }

    public static Complaint fromJSON(String json) {
        Map<String, Object> map = SimpleJSON.parse(json);
        // Handle both 'category' and 'categoryName' for robustness
        String category = (String) map.getOrDefault("categoryName", map.get("category"));

        return new Complaint(
                ((Number) map.getOrDefault("complaintId", 0)).intValue(),
                ((Number) map.getOrDefault("studentId", 0)).intValue(),
                (String) map.getOrDefault("title", ""),
                (String) map.getOrDefault("description", ""),
                category,
                (String) map.getOrDefault("priority", "MEDIUM"),
                (String) map.getOrDefault("status", "PENDING"),
                (String) map.getOrDefault("submissionDate", ""),
                (String) map.getOrDefault("resolutionDate", null)
        );
    }
}