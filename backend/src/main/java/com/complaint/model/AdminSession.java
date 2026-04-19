package main.java.com.complaint.model;

import main.java.com.complaint.util.SimpleJSON;
import java.util.*;

public class AdminSession implements SimpleJSON.JSONable {
    private String sessionId;
    private int adminId;
    private String createdAt;
    private String expiresAt;
    private String ipAddress;
    private String userAgent;

    public AdminSession(String sessionId, int adminId, String createdAt,
                        String expiresAt, String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.adminId = adminId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public int getAdminId() { return adminId; }
    public String getCreatedAt() { return createdAt; }
    public String getExpiresAt() { return expiresAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }

    @Override
    public String toJSON() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", sessionId);
        map.put("adminId", adminId);
        map.put("createdAt", createdAt);
        map.put("expiresAt", expiresAt);
        map.put("ipAddress", ipAddress);
        map.put("userAgent", userAgent);
        return SimpleJSON.toJSON(map);
    }

    public static AdminSession fromJSON(String json) {
        Map<String, Object> map = SimpleJSON.parse(json);
        return new AdminSession(
                (String) map.getOrDefault("sessionId", ""),
                ((Number) map.getOrDefault("adminId", 0)).intValue(),
                (String) map.getOrDefault("createdAt", ""),
                (String) map.getOrDefault("expiresAt", ""),
                (String) map.getOrDefault("ipAddress", ""),
                (String) map.getOrDefault("userAgent", "")
        );
    }
}