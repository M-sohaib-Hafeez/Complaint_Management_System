-- Backup Script for Complaint Management System Database
SET @backup_date = DATE_FORMAT(NOW(), '%Y%m%d_%H%i%s');

-- Create backup file name
SET @backup_file = CONCAT('/tmp/complaint_db_backup_', @backup_date, '.sql');

-- Export data (run this from command line, not in MySQL)
-- mysqldump -u root -p complaint_db > complaint_db_backup.sql

-- Quick statistics for verification
SELECT
    'Current Database Statistics' as title,
    '' as '',
    CONCAT('Students: ', COUNT(*)) as students
FROM students
UNION ALL
SELECT
    '',
    '',
    CONCAT('Complaints: ', COUNT(*))
FROM complaints
UNION ALL
SELECT
    '',
    '',
    CONCAT('Pending: ', SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END))
FROM complaints
UNION ALL
SELECT
    '',
    '',
    CONCAT('In Progress: ', SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END))
FROM complaints
UNION ALL
SELECT
    '',
    '',
    CONCAT('Resolved: ', SUM(CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END))
FROM complaints;

-- Backup verification query
SELECT
    'Backup Verification' as check_item,
    IF(COUNT(*) > 0, '✓', '✗') as status
FROM information_schema.tables
WHERE table_schema = 'complaint_db'
UNION ALL
SELECT
    'All tables exist',
    IF(COUNT(*) = 5, '✓', '✗')
FROM information_schema.tables
WHERE table_schema = 'complaint_db'
AND table_name IN ('departments', 'students', 'categories', 'complaints', 'complaint_comments');