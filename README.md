# 🎓 Complaint Management System

A full-stack **Student Complaint Management System** with a Java HTTP backend and a redesigned dark-themed interactive frontend.

---

## 📁 Project Structure

```
ComplaintManagement/
├── backend/
│   └── src/main/java/com/complaint/
│       ├── Main.java                  — Server entry point (port 8080)
│       ├── database/
│       │   ├── DatabaseConnection.java
│       │   └── DAO.java
│       ├── handler/
│       │   └── RequestHandler.java
│       ├── model/
│       │   ├── Complaint.java
│       │   ├── ComplaintNode.java
│       │   ├── Student.java
│       │   ├── AdminUser.java
│       │   └── AdminSession.java
│       ├── queue/
│       │   └── ComplaintQueueManager.java
│       └── util/
│           ├── PasswordUtil.java
│           ├── SimpleJSON.java
│           └── GeneratePassword.java
├── frontend/
│   ├── index.html          — Student portal (Dashboard, Submit, Track)
│   ├── Adminlogin.html     — Admin login
│   ├── admin.html          — Admin management panel
│   ├── css/
│   │   └── style.css
│   └── js/
│       ├── main.js
│       ├── auth.js
│       ├── dashboard.js
│       ├── submit.js
│       ├── track.js
│       └── admin.js
├── database/
│   ├── schema.sql          — Full DB schema + tables + views
│   ├── sample-data.sql
│   └── backup-script.sql
└── pom.xml
```

---

## ⚙️ Setup & Running

### 1. Database Setup
```sql
-- In MySQL:
SOURCE database/schema.sql;
```
Default admin credentials (change after first login):
- **Username:** `admin`
- **Password:** `admin123`

### 2. Configure Database Connection
Edit `backend/src/main/java/com/complaint/database/DatabaseConnection.java`:
```java
private static final String DB_PASSWORD = "your_mysql_password";
```

### 3. Run the Backend
```bash
# Using Maven
mvn compile exec:java -Dexec.mainClass="main.java.com.complaint.Main"

# Or compile manually and run Main.java
```
Server starts on **http://localhost:8080**

### 4. Open the Frontend
Open `frontend/index.html` in a browser, or serve with:
```bash
cd frontend
python -m http.server 8000
# Then open http://localhost:8000
```

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET  | `/api/stats` | System statistics |
| GET  | `/api/complaints/all` | All complaints |
| GET  | `/api/complaints/:id` | Single complaint by ID |
| GET  | `/api/complaints/student/:email` | Complaints by student email |
| POST | `/api/complaints/submit` | Submit new complaint |
| PUT  | `/api/complaints/:id/status` | Update complaint status |
| PUT  | `/api/complaints/:id/resolve` | Resolve a complaint |
| DELETE | `/api/complaints/:id` | Delete complaint (admin only) |
| POST | `/api/auth/login` | Admin login |
| POST | `/api/auth/logout` | Admin logout |
| GET  | `/api/auth/validate` | Validate session |

---

## 🔒 Security Fixes (v2.0)

- Removed hardcoded DB password from console logs
- Fixed thread-unsafe `HashMap` → `ConcurrentHashMap` for sessions
- Added request body size limit (1MB max)
- Added status value whitelist validation
- Added DELETE endpoint with admin session check
- Sanitized error responses (no internal stack traces exposed)
- Fixed auth redirect pointing to wrong filename

## 🎨 Frontend Changes (v2.0)

- Complete redesign: dark industrial theme (Space Mono + Syne fonts)
- Per-field inline validation errors on submit form
- Live search + filter by status/priority in admin panel
- Smooth number animation on stat cards
- Mobile responsive hamburger nav
- Character counter on description field
- Interactive priority pill selector
- Proper empty states and loading states

---

## 🧠 Data Structures Used

- **Priority Queue** — HIGH priority complaints processed first
- **Regular Queue (LinkedList)** — MEDIUM/LOW complaints
- **ConcurrentHashMap** — Thread-safe active session tracking

---

## 👨‍💻 Author

**Sohaib Hafeez**
Roll No: 24F-CS-085 | Section A2
BS Computer Science — Dawood University of Engineering and Technology (DUET), Karachi
