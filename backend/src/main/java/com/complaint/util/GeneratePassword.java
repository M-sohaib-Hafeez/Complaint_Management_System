package main.java.com.complaint.util;

public class GeneratePassword {
    public static void main(String[] args) {
        String password = "admin123";
        String hash = main.java.com.complaint.util.PasswordUtil.hashPassword(password);

        System.out.println("=================================");
        System.out.println("Password Hash Generator");
        System.out.println("=================================");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("\n=================================");
        System.out.println("Run this SQL command:");
        System.out.println("=================================");
        System.out.println("UPDATE admin_users");
        System.out.println("SET password_hash = '" + hash + "'");
        System.out.println("WHERE username = 'admin';");
        System.out.println("=================================");

        // Test verification
        boolean verified = main.java.com.complaint.util.PasswordUtil.verifyPassword(password, hash);
        System.out.println("\nVerification Test: " + (verified ? "✅ PASSED" : "❌ FAILED"));
    }
}