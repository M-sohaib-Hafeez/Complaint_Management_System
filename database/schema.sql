-- =====================================================
-- Student Complaint Management System Database Schema
-- =====================================================
DROP DATABASE IF EXISTS complaint_db;
CREATE DATABASE complaint_db;
USE complaint_db;

-- =====================================================
-- TABLE: departments
-- =====================================================
CREATE TABLE departments (
    department_id INT PRIMARY KEY AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    department_code VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- TABLE: students
-- =====================================================
CREATE TABLE students (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department_id INT,
    semester INT NOT NULL,
    phone VARCHAR(20),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    INDEX idx_email (email),
    INDEX idx_department (department_id)
);

-- =====================================================
-- TABLE: categories
-- =====================================================
CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL UNIQUE,
    category_description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- TABLE: complaints
-- =====================================================
CREATE TABLE complaints (
    complaint_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    category_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') DEFAULT 'PENDING',
    submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolution_date TIMESTAMP NULL,
    assigned_to VARCHAR(100),
    resolution_notes TEXT,
    satisfaction_rating INT,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    INDEX idx_student (student_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_submission_date (submission_date),
    INDEX idx_status_priority (status, priority)
);

-- =====================================================
-- TABLE: complaint_comments
-- =====================================================
CREATE TABLE complaint_comments (
    comment_id INT PRIMARY KEY AUTO_INCREMENT,
    complaint_id INT NOT NULL,
    commenter_name VARCHAR(100) NOT NULL,
    commenter_type ENUM('STUDENT', 'ADMIN', 'STAFF') NOT NULL,
    comment_text TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    INDEX idx_complaint (complaint_id)
);

-- =====================================================
-- TABLE: complaint_history
-- =====================================================
CREATE TABLE complaint_history (
    history_id INT PRIMARY KEY AUTO_INCREMENT,
    complaint_id INT NOT NULL,
    old_status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'),
    new_status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'),
    changed_by VARCHAR(100) NOT NULL,
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id) ON DELETE CASCADE,
    INDEX idx_complaint (complaint_id)
);

-- ============================================
-- Complaint Management System - Database Updates
-- Run this script to add admin authentication
-- ============================================


-- 1. Add admin users table
CREATE TABLE IF NOT EXISTS admin_users (
    admin_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'ADMIN',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Add sessions table for admin authentication
CREATE TABLE IF NOT EXISTS admin_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    admin_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (admin_id) REFERENCES admin_users(admin_id) ON DELETE CASCADE,
    INDEX idx_admin_id (admin_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Add last_updated column to complaints (if not exists)
ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 4. Insert default admin user
-- Username: admin
-- Password: admin123
-- NOTE: Change this password immediately after first login!
INSERT INTO admin_users (username, password_hash, full_name, email, role)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System Administrator',
    'admin@complaint.com',
    'ADMIN'
)
ON DUPLICATE KEY UPDATE username=username;

-- 5. Optional: Add additional admin users
-- Uncomment and modify as needed
/*
INSERT INTO admin_users (username, password_hash, full_name, email, role)
VALUES (
    'manager',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Complaint Manager',
    'manager@complaint.com',
    'ADMIN'
);
*/

-- 6. Create view for active sessions
CREATE OR REPLACE VIEW active_admin_sessions AS
SELECT
    s.session_id,
    s.admin_id,
    a.username,
    a.full_name,
    s.created_at,
    s.expires_at,
    s.ip_address,
    TIMESTAMPDIFF(MINUTE, NOW(), s.expires_at) as minutes_remaining
FROM admin_sessions s
JOIN admin_users a ON s.admin_id = a.admin_id
WHERE s.expires_at > NOW()
AND a.is_active = TRUE;

-- 7. Clean up expired sessions (run periodically)
DELETE FROM admin_sessions WHERE expires_at < NOW();

-- Verification queries
SELECT 'Admin Users Table' as 'Status';
SELECT * FROM admin_users;

SELECT 'Tables Created Successfully' as 'Status';
SHOW TABLES LIKE '%admin%';

-- Display default credentials
SELECT
    '⚠️ DEFAULT ADMIN CREDENTIALS' as 'IMPORTANT',
    'Username: admin' as 'Credential_1',
    'Password: admin123' as 'Credential_2',
    '🔒 CHANGE THIS PASSWORD IMMEDIATELY!' as 'Security_Warning';

-- =====================================================
-- STORED PROCEDURES
-- =====================================================
DELIMITER //

CREATE PROCEDURE sp_submit_complaint(
    IN p_student_id INT,
    IN p_category_name VARCHAR(50),
    IN p_title VARCHAR(200),
    IN p_description TEXT,
    IN p_priority ENUM('LOW', 'MEDIUM', 'HIGH')
)
BEGIN
    DECLARE v_category_id INT;

    SELECT category_id INTO v_category_id
    FROM categories
    WHERE category_name = p_category_name;

    INSERT INTO complaints (student_id, category_id, title, description, priority)
    VALUES (p_student_id, v_category_id, p_title, p_description, p_priority);

    SELECT LAST_INSERT_ID() as complaint_id;
END //

CREATE PROCEDURE sp_update_complaint_status(
    IN p_complaint_id INT,
    IN p_new_status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'),
    IN p_changed_by VARCHAR(100),
    IN p_change_reason TEXT,
    IN p_resolution_notes TEXT
)
BEGIN
    DECLARE v_old_status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');

    SELECT status INTO v_old_status
    FROM complaints
    WHERE complaint_id = p_complaint_id;

    UPDATE complaints
    SET status = p_new_status,
        resolution_date = CASE WHEN p_new_status = 'RESOLVED' THEN CURRENT_TIMESTAMP ELSE resolution_date END,
        resolution_notes = COALESCE(p_resolution_notes, resolution_notes)
    WHERE complaint_id = p_complaint_id;

    INSERT INTO complaint_history (complaint_id, old_status, new_status, changed_by, change_reason)
    VALUES (p_complaint_id, v_old_status, p_new_status, p_changed_by, p_change_reason);
END //

CREATE PROCEDURE sp_get_next_complaint()
BEGIN
    -- Try to get HIGH priority PENDING complaints first
    SELECT c.*, s.student_name, s.email, cat.category_name
    FROM complaints c
    JOIN students s ON c.student_id = s.student_id
    JOIN categories cat ON c.category_id = cat.category_id
    WHERE c.status = 'PENDING' AND c.priority = 'HIGH'
    ORDER BY c.submission_date ASC
    LIMIT 1;

    -- If no HIGH priority, get MEDIUM
    IF FOUND_ROWS() = 0 THEN
        SELECT c.*, s.student_name, s.email, cat.category_name
        FROM complaints c
        JOIN students s ON c.student_id = s.student_id
        JOIN categories cat ON c.category_id = cat.category_id
        WHERE c.status = 'PENDING' AND c.priority = 'MEDIUM'
        ORDER BY c.submission_date ASC
        LIMIT 1;
    END IF;

    -- If no MEDIUM, get LOW
    IF FOUND_ROWS() = 0 THEN
        SELECT c.*, s.student_name, s.email, cat.category_name
        FROM complaints c
        JOIN students s ON c.student_id = s.student_id
        JOIN categories cat ON c.category_id = cat.category_id
        WHERE c.status = 'PENDING' AND c.priority = 'LOW'
        ORDER BY c.submission_date ASC
        LIMIT 1;
    END IF;
END //

DELIMITER ;

-- =====================================================
-- TRIGGERS
-- =====================================================
DELIMITER //

CREATE TRIGGER trg_set_resolution_date
BEFORE UPDATE ON complaints
FOR EACH ROW
BEGIN
    IF NEW.status = 'RESOLVED' AND OLD.status != 'RESOLVED' THEN
        SET NEW.resolution_date = CURRENT_TIMESTAMP;
    END IF;
END //

DELIMITER ;

-- =====================================================
-- VIEWS
-- =====================================================
CREATE VIEW v_active_complaints AS
SELECT
    c.complaint_id,
    c.title,
    c.description,
    c.priority,
    c.status,
    s.student_id,
    s.student_name,
    s.email,
    d.department_name,
    s.semester,
    cat.category_name,
    c.submission_date,
    c.last_updated,
    c.assigned_to,
    TIMESTAMPDIFF(DAY, c.submission_date, CURRENT_TIMESTAMP) as days_open
FROM complaints c
JOIN students s ON c.student_id = s.student_id
JOIN departments d ON s.department_id = d.department_id
JOIN categories cat ON c.category_id = cat.category_id
WHERE c.status IN ('PENDING', 'IN_PROGRESS');

CREATE VIEW v_category_statistics AS
SELECT
    cat.category_name,
    COUNT(c.complaint_id) as total_complaints,
    SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) as pending,
    SUM(CASE WHEN c.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress,
    SUM(CASE WHEN c.status = 'RESOLVED' THEN 1 ELSE 0 END) as resolved,
    ROUND(AVG(CASE WHEN c.resolution_date IS NOT NULL
        THEN TIMESTAMPDIFF(HOUR, c.submission_date, c.resolution_date)
        END), 2) as avg_resolution_hours
FROM categories cat
LEFT JOIN complaints c ON cat.category_id = c.category_id
GROUP BY cat.category_id, cat.category_name;

-- =====================================================
-- SAMPLE DATA
-- =====================================================
INSERT INTO departments (department_name, department_code) VALUES
('Computer Science', 'CS'),
('Electrical Engineering', 'EE'),
('Mechanical Engineering', 'ME'),
('Civil Engineering', 'CE'),
('Business Administration', 'BA'),
('Mathematics', 'MATH'),
('Physics', 'PHY'),
('Chemistry', 'CHEM');

INSERT INTO categories (category_name, category_description) VALUES
('Technical', 'Technical issues including computers, networks, and software'),
('Academic', 'Academic related complaints including courses, exams, and grades'),
('Hostel', 'Hostel facilities and accommodation related issues'),
('Library', 'Library services, books, and facilities'),
('Transportation', 'Transportation and shuttle service complaints'),
('Cafeteria', 'Cafeteria food quality and service'),
('Administration', 'Administrative and bureaucratic issues'),
('Other', 'Other miscellaneous complaints');

INSERT INTO students (student_name, email, department_id, semester, phone) VALUES
('John Doe', 'john@example.com', 1, 3, '+1234567890'),
('Jane Smith', 'jane@example.com', 2, 4, '+1234567891'),
('Bob Wilson', 'bob@example.com', 3, 2, '+1234567892'),
('Alice Johnson', 'alice@example.com', 1, 5, '+1234567893'),
('Charlie Brown', 'charlie@example.com', 4, 3, '+1234567894');

INSERT INTO complaints (student_id, category_id, title, description, priority, status, assigned_to) VALUES
(1, 1, 'Network Issue', 'WiFi keeps disconnecting in computer lab', 'HIGH', 'PENDING', NULL),
(1, 1, 'Projector Problem', 'Projector in Room 301 not working', 'MEDIUM', 'IN_PROGRESS', 'Tech Support'),
(2, 3, 'Library Book Unavailable', 'Required book unavailable', 'MEDIUM', 'PENDING', NULL),
(3, 4, 'No Hot Water', 'No hot water for 3 days', 'HIGH', 'RESOLVED', 'Maintenance'),
(4, 2, 'Exam Schedule Conflict', 'Two exams on same day', 'HIGH', 'IN_PROGRESS', 'Academic Office'),
(5, 6, 'Food Quality Issue', 'Cafeteria food quality poor', 'LOW', 'PENDING', NULL);

INSERT INTO complaint_comments (complaint_id, commenter_name, commenter_type, comment_text, is_internal) VALUES
(1, 'John Doe', 'STUDENT', 'This issue is affecting multiple students', FALSE),
(2, 'Tech Support', 'STAFF', 'Projector bulb ordered, will arrive in 2 days', FALSE),
(4, 'Maintenance Team', 'STAFF', 'Boiler repaired, hot water restored', FALSE);

INSERT INTO complaint_history (complaint_id, old_status, new_status, changed_by) VALUES
(2, 'PENDING', 'IN_PROGRESS', 'System'),
(4, 'PENDING', 'IN_PROGRESS', 'System'),
(4, 'IN_PROGRESS', 'RESOLVED', 'Maintenance Team');

SELECT 'Database schema created successfully!' as message;