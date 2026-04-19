package main.java.com.complaint.model;

public class ComplaintNode implements Comparable<ComplaintNode> {
    private final Complaint complaint;
    private final int priorityValue;

    public ComplaintNode(Complaint complaint) {
        this.complaint = complaint;
        this.priorityValue = getPriorityValue(complaint.getPriority());
    }

    private int getPriorityValue(String priority) {
        switch(priority.toUpperCase()) {
            case "HIGH": return 1;
            case "MEDIUM": return 2;
            case "LOW": return 3;
            default: return 2;
        }
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public int getPriorityValue() {
        return priorityValue;
    }

    @Override
    public int compareTo(ComplaintNode other) {
        return Integer.compare(this.priorityValue, other.priorityValue);
    }
}