-- Additional Sample Data for Testing
USE complaint_db;

-- Add more students
INSERT INTO students (student_name, email, department_id, semester, phone) VALUES
('David Miller', 'david@example.com', 2, 6, '+1234567895'),
('Emma Watson', 'emma@example.com', 5, 4, '+1234567896'),
('Frank Ocean', 'frank@example.com', 1, 7, '+1234567897'),
('Grace Lee', 'grace@example.com', 3, 3, '+1234567898'),
('Henry Ford', 'henry@example.com', 4, 5, '+1234567899');

-- Add more complaints with various priorities and statuses
INSERT INTO complaints (student_id, category_id, title, description, priority, status, assigned_to) VALUES
(6, 5, 'Bus Schedule Issue', 'Evening bus always late by 30 minutes', 'MEDIUM', 'PENDING', NULL),
(7, 7, 'Document Processing Delay', 'Transcript request pending for 2 weeks', 'HIGH', 'IN_PROGRESS', 'Admin Office'),
(8, 1, 'Lab Computer Down', 'Computer #12 in CS lab not working', 'MEDIUM', 'RESOLVED', 'IT Department'),
(9, 8, 'Sports Equipment Broken', 'Basketball court needs repair', 'LOW', 'PENDING', NULL),
(10, 3, 'Hostel Room Maintenance', 'Room 205 AC not working', 'HIGH', 'IN_PROGRESS', 'Maintenance'),
(6, 2, 'Course Registration Issue', 'Cannot register for required course', 'MEDIUM', 'PENDING', NULL),
(7, 4, 'Library Hours', 'Library should open earlier during exams', 'LOW', 'PENDING', NULL),
(8, 6, 'Cafeteria Hygiene', 'Unclean tables and utensils', 'MEDIUM', 'RESOLVED', 'Cafeteria Manager'),
(9, 1, 'Software License Expired', 'MATLAB license expired', 'HIGH', 'IN_PROGRESS', 'IT Department'),
(10, 5, 'Shuttle Bus Capacity', 'Shuttle bus too crowded during peak hours', 'MEDIUM', 'PENDING', NULL);

-- Add more comments
INSERT INTO complaint_comments (complaint_id, commenter_name, commenter_type, comment_text, is_internal) VALUES
(7, 'Admin Staff', 'STAFF', 'Processing your transcript, will be ready tomorrow', FALSE),
(8, 'IT Support', 'STAFF', 'Computer repaired and tested', FALSE),
(11, 'Maintenance', 'STAFF', 'AC technician assigned, will visit tomorrow', FALSE),
(13, 'Cafeteria Manager', 'STAFF', 'Cleaning schedule improved, staff retrained', FALSE),
(14, 'IT Department', 'STAFF', 'New license purchased, will be activated today', TRUE);

-- Add status change history
INSERT INTO complaint_history (complaint_id, old_status, new_status, changed_by, change_reason) VALUES
(6, 'PENDING', 'IN_PROGRESS', 'Transport Dept', 'Assigned to transport team'),
(7, 'PENDING', 'IN_PROGRESS', 'Admin Office', 'Urgent request for job application'),
(8, 'PENDING', 'IN_PROGRESS', 'IT Dept', 'Hardware issue identified'),
(8, 'IN_PROGRESS', 'RESOLVED', 'IT Dept', 'Hardware replaced successfully'),
(10, 'PENDING', 'IN_PROGRESS', 'Maintenance', 'AC issue reported'),
(13, 'PENDING', 'IN_PROGRESS', 'Cafeteria', 'Hygiene complaint received'),
(13, 'IN_PROGRESS', 'RESOLVED', 'Cafeteria', 'Cleaning procedures improved'),
(14, 'PENDING', 'IN_PROGRESS', 'IT Dept', 'Software license issue');

-- Update some satisfaction ratings
UPDATE complaints SET satisfaction_rating = 4 WHERE complaint_id = 4;
UPDATE complaints SET satisfaction_rating = 5 WHERE complaint_id = 8;
UPDATE complaints SET satisfaction_rating = 3 WHERE complaint_id = 13;

SELECT 'Sample data inserted successfully!' as message;
SELECT COUNT(*) as total_students FROM students;
SELECT COUNT(*) as total_complaints FROM complaints;
SELECT status, COUNT(*) as count FROM complaints GROUP BY status;