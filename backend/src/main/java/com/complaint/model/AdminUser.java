package main.java.com.complaint.model;

import main.java.com.complaint.util.SimpleJSON;
import java.util.*;

public class AdminUser implements SimpleJSON.JSONable {
    private int adminId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String role;
    private boolean isActive;
    private String createdAt;
    private String lastLogin;

    // Constructor for new admin (without ID)
    public AdminUser(String username, String passwordHash, String fullName,
                     String email, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.isActive = true;
    }

    // Constructor from database
    public AdminUser(int adminId, String username, String passwordHash,
                     String fullName, String email, String role,
                     boolean isActive, String createdAt, String lastLogin) {
        this.adminId = adminId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    // Getters
    public int getAdminId() { return adminId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return isActive; }
    public String getCreatedAt() { return createdAt; }
    public String getLastLogin() { return lastLogin; }

    // Setters
    public void setAdminId(int adminId) { this.adminId = adminId; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toJSON() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("adminId", adminId);
        map.put("username", username);
        // Never expose password hash in JSON
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("role", role);
        map.put("isActive", isActive);
        map.put("createdAt", createdAt);
        map.put("lastLogin", lastLogin);
        return SimpleJSON.toJSON(map);
    }

    // Safe JSON (without sensitive data)
    public String toSafeJSON() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("adminId", adminId);
        map.put("username", username);
        map.put("fullName", fullName);
        map.put("role", role);
        return SimpleJSON.toJSON(map);
    }

    public static AdminUser fromJSON(String json) {
        Map<String, Object> map = SimpleJSON.parse(json);
        return new AdminUser(
                ((Number) map.getOrDefault("adminId", 0)).intValue(),
                (String) map.getOrDefault("username", ""),
                (String) map.getOrDefault("passwordHash", ""),
                (String) map.getOrDefault("fullName", ""),
                (String) map.getOrDefault("email", ""),
                (String) map.getOrDefault("role", "ADMIN"),
                (Boolean) map.getOrDefault("isActive", true),
                (String) map.getOrDefault("createdAt", ""),
                (String) map.getOrDefault("lastLogin", null)
        );
    }
}