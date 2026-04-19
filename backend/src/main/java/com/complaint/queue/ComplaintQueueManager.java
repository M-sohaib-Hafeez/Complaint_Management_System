package main.java.com.complaint.queue;

import main.java.com.complaint.model.Complaint;
import main.java.com.complaint.model.ComplaintNode;
import main.java.com.complaint.util.SimpleJSON;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ComplaintQueueManager {
    private static final PriorityQueue<ComplaintNode> priorityQueue = new PriorityQueue<>();
    private static final Queue<Complaint> regularQueue = new LinkedList<>();
    private static final Map<Integer, Complaint> allComplaints = new ConcurrentHashMap<>();

    // Initialize with sample data
    static {
        initializeSampleComplaints();
    }

    private static void initializeSampleComplaints() {
        System.out.println("📋 Initializing complaint queues...");

        // Add sample complaints to queues
        Complaint c1 = new Complaint(1001, 101, "Network Issue",
                "WiFi keeps disconnecting", "Technical", "HIGH");
        Complaint c2 = new Complaint(1002, 102, "Projector Problem",
                "Projector not working", "Technical", "MEDIUM");
        Complaint c3 = new Complaint(1003, 103, "Library Book",
                "Required book unavailable", "Academic", "HIGH");
        Complaint c4 = new Complaint(1004, 101, "Hostel Water",
                "No hot water", "Hostel", "LOW");

        addComplaint(c1);
        addComplaint(c2);
        addComplaint(c3);
        addComplaint(c4);

        System.out.println("✅ Queue initialization complete");
        System.out.println("   Priority Queue: " + priorityQueue.size() + " complaints");
        System.out.println("   Regular Queue: " + regularQueue.size() + " complaints");
    }

    public static void addComplaint(Complaint complaint) {
        allComplaints.put(complaint.getId(), complaint);

        if (complaint.getPriority().equalsIgnoreCase("HIGH")) {
            priorityQueue.offer(new ComplaintNode(complaint));
            System.out.println("🚨 Added HIGH priority complaint to priority queue: " + complaint.getTitle());
        } else {
            regularQueue.offer(complaint);
            System.out.println("📝 Added " + complaint.getPriority() + " priority complaint to regular queue: " + complaint.getTitle());
        }
    }

    public static Complaint getNextComplaint() {
        // Check priority queue first (HIGH priority complaints)
        if (!priorityQueue.isEmpty()) {
            ComplaintNode node = priorityQueue.poll();
            if (node != null) {
                Complaint complaint = node.getComplaint();
                complaint.setStatus("IN_PROGRESS");
                System.out.println("🔄 Processing HIGH priority complaint: " + complaint.getTitle());
                return complaint;
            }
        }

        // Then check regular queue (MEDIUM/LOW priority)
        if (!regularQueue.isEmpty()) {
            Complaint complaint = regularQueue.poll();
            complaint.setStatus("IN_PROGRESS");
            System.out.println("🔄 Processing " + complaint.getPriority() + " priority complaint: " + complaint.getTitle());
            return complaint;
        }

        System.out.println("📭 No complaints in queue");
        return null;
    }

    public static boolean resolveComplaint(int complaintId) {
        Complaint complaint = allComplaints.get(complaintId);
        if (complaint != null && !complaint.getStatus().equals("RESOLVED")) {
            complaint.setStatus("RESOLVED");
            complaint.setResolutionDate(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Remove from queues
            removeFromQueues(complaintId);

            System.out.println("✅ Resolved complaint: " + complaint.getTitle());
            return true;
        }
        return false;
    }

    private static void removeFromQueues(int complaintId) {
        // Remove from priority queue
        priorityQueue.removeIf(node -> node.getComplaint().getId() == complaintId);

        // Remove from regular queue
        regularQueue.removeIf(c -> c.getId() == complaintId);
    }

    public static void removeComplaint(int complaintId) {
        allComplaints.remove(complaintId);
        removeFromQueues(complaintId);
    }

    public static int getPriorityQueueSize() {
        return priorityQueue.size();
    }

    public static int getRegularQueueSize() {
        return regularQueue.size();
    }

    public static int getTotalPending() {
        return priorityQueue.size() + regularQueue.size();
    }

    public static List<Complaint> getPriorityQueueComplaints() {
        List<Complaint> list = new ArrayList<>();
        for (ComplaintNode node : priorityQueue) {
            list.add(node.getComplaint());
        }
        return list;
    }

    public static List<Complaint> getRegularQueueComplaints() {
        return new ArrayList<>(regularQueue);
    }

    public static Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("priorityQueueSize", priorityQueue.size());
        status.put("regularQueueSize", regularQueue.size());
        status.put("totalPending", getTotalPending());

        // Add complaint details
        List<Map<String, Object>> priorityComplaints = new ArrayList<>();
        for (ComplaintNode node : priorityQueue) {
            priorityComplaints.add(SimpleJSON.parse(node.getComplaint().toJSON()));
        }
        status.put("priorityComplaints", priorityComplaints);

        List<Map<String, Object>> regularComplaints = new ArrayList<>();
        for (Complaint complaint : regularQueue) {
            regularComplaints.add(SimpleJSON.parse(complaint.toJSON()));
        }
        status.put("regularComplaints", regularComplaints);

        return status;
    }
}