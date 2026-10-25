# Student Management System

A small student-directory website built with static HTML/CSS/JavaScript, plain Java, and JDBC. The quickest local run uses an embedded H2 database and the JDK's built-in HTTP server, so it does not require MySQL, Maven, or Tomcat. A MySQL + Servlet + Tomcat deployment is also supported.

## Requirements

- JDK 11 or newer
- Internet access the first time you start the local app, to download the H2 JDBC driver
- For the optional MySQL/Servlet deployment: Apache Maven 3.8+, MySQL 8+, and Apache Tomcat 10.1

In VS Code, install the recommended Java and Maven extensions if prompted, then run
**Java: Import Java Projects in Workspace** after opening the repository root folder.
The backend Maven module targets Jakarta Servlet 6, so deploy it to Tomcat 10.1 (not
Tomcat 9).

## Run locally (recommended)

From PowerShell in the project root, run:

```powershell
.\run-local.ps1
```

The script downloads the small H2 JDBC driver into your user profile (not the repository), compiles the Java backend, creates `data/student_management.mv.db`, and starts the site. Open:

```text
http://localhost:8080/
```

Keep the PowerShell window open while using the app; press Ctrl+C there to stop the server. Records are saved locally and remain after restarting. To choose another port, set `$env:STUDENT_PORT` before starting, for example `$env:STUDENT_PORT = "8081"`.

## Optional MySQL + Tomcat deployment

Start MySQL and run the included schema from the project root:

```powershell
cmd /c "mysql -u root -p < database\schema.sql"
```

The backend defaults to MySQL when run as a servlet. Set these environment variables in the environment used to start Tomcat if your connection details differ:

```powershell
$env:STUDENT_DB_USER = "root"
$env:STUDENT_DB_PASSWORD = "your-local-mysql-password"
```

Build the WAR from the project root:

```powershell
mvn clean package
```

Copy `backend/target/student-management.war` to Tomcat 10.1's `webapps` directory, start Tomcat, then open:

```text
http://localhost:8080/student-management/
```

Maven downloads the MySQL Connector/J and Jakarta Servlet dependencies. Tomcat provides the Servlet API. The frontend and servlet are hosted by the same web app; no separate frontend server or CORS setup is required.

## Project files

```text
database/schema.sql
frontend/
  index.html
  style.css
  script.js
backend/
  pom.xml
  src/main/java/com/studentmanagement/
    DatabaseConnection.java
    StudentDAO.java
    StudentServlet.java
    LocalServer.java
  src/main/webapp/WEB-INF/web.xml
run-local.ps1
```

## Features and request flow

- List student records and view summary counts.
- Add, edit, and delete records from the dashboard.
- The frontend sends URL-encoded form data to `POST /api/students`; `action` is `add`, `update`, or `delete`. `GET /api/students` returns a JSON student list.
- `StudentDAO` uses JDBC `PreparedStatement`s for every SQL operation. Each DAO operation disables auto-commit, commits on success, and rolls back if an SQL or runtime exception occurs. The standalone local mode uses embedded H2; the Servlet deployment uses MySQL.
- The servlet validates input and returns clear HTTP errors. Email addresses must be unique.

The API is hosted at `/api/students`. Do not open `frontend/index.html` directly as a `file:` URL: the backend must be running. If you change the WAR filename or deploy it under another Tomcat context, keep the frontend and servlet together in that web application.
