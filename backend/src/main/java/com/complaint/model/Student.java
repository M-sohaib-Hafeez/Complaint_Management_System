package main.java.com.complaint.model;

import main.java.com.complaint.util.SimpleJSON;

import java.util.*;

public class Student implements SimpleJSON.JSONable {
    private int id;
    private String name;
    private String email;
    private String department;
    private int semester;

    public Student(int id, String name, String email, String department, int semester) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.semester = semester;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public int getSemester() {
        return semester;
    }

    @Override
    public String toJSON() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studentId", id);
        map.put("studentName", name);
        map.put("email", email);
        map.put("department", department);
        map.put("semester", semester);
        return SimpleJSON.toJSON(map);
    }

    public static Student fromJSON(String json) {
        Map<String, Object> map = SimpleJSON.parse(json);
        return new Student(
                ((Number) map.getOrDefault("studentId", 0)).intValue(),
                (String) map.getOrDefault("studentName", ""),
                (String) map.getOrDefault("email", ""),
                (String) map.getOrDefault("department", ""),
                ((Number) map.getOrDefault("semester", 1)).intValue()
        );
    }
}