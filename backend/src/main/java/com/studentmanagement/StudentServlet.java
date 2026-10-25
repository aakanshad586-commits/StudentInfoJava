package com.studentmanagement;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import com.studentmanagement.StudentDAO.Student;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/students")
public class StudentServlet extends HttpServlet {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeStudents(response, studentDAO.findAll());
        } catch (SQLException exception) {
            log("Unable to load student records.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Student records could not be loaded. Check the server and database configuration.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            String action = requiredParameter(request, "action");
            switch (action) {
                case "add":
                    studentDAO.create(readStudent(request, 0));
                    writeMessage(response, HttpServletResponse.SC_CREATED, "Student added.");
                    break;
                case "update":
                    Student updated = readStudent(request, parseId(request));
                    if (!studentDAO.update(updated)) {
                        writeError(response, HttpServletResponse.SC_NOT_FOUND, "Student record not found.");
                        return;
                    }
                    writeMessage(response, HttpServletResponse.SC_OK, "Student updated.");
                    break;
                case "delete":
                    if (!studentDAO.delete(parseId(request))) {
                        writeError(response, HttpServletResponse.SC_NOT_FOUND, "Student record not found.");
                        return;
                    }
                    writeMessage(response, HttpServletResponse.SC_OK, "Student deleted.");
                    break;
                default:
                    throw new IllegalArgumentException("Choose a valid student action.");
            }
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            log("Unable to save student record.", exception);
            boolean duplicateEmail = exception.getErrorCode() == 1062
                    || (exception.getSQLState() != null && exception.getSQLState().startsWith("23"));
            String message = duplicateEmail
                    ? "A student with that email address already exists."
                    : "The student record could not be saved. Check the server and database configuration.";
            int status = duplicateEmail
                    ? HttpServletResponse.SC_CONFLICT
                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            writeError(response, status, message);
        }
    }

    private Student readStudent(HttpServletRequest request, long id) {
        String fullName = requiredParameter(request, "fullName");
        String email = requiredParameter(request, "email");
        String course = requiredParameter(request, "course");
        String yearValue = requiredParameter(request, "year");

        if (fullName.length() > 100 || email.length() > 150 || course.length() > 100) {
            throw new IllegalArgumentException("Name, email, or course exceeds the allowed length.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        int year;
        try {
            year = Integer.parseInt(yearValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Choose an academic year from 1 to 4.");
        }
        if (year < 1 || year > 4) {
            throw new IllegalArgumentException("Choose an academic year from 1 to 4.");
        }
        return new Student(id, fullName, email, course, year);
    }

    private long parseId(HttpServletRequest request) {
        try {
            long id = Long.parseLong(requiredParameter(request, "id"));
            if (id < 1) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("A valid student ID is required.");
        }
    }

    private String requiredParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Please provide " + name + ".");
        }
        return value.trim();
    }

    private void writeStudents(HttpServletResponse response, List<Student> students) throws IOException {
        StringBuilder json = new StringBuilder("{\"students\":[");
        for (int index = 0; index < students.size(); index++) {
            if (index > 0) json.append(',');
            Student student = students.get(index);
            json.append("{\"id\":").append(student.getId())
                    .append(",\"fullName\":").append(jsonString(student.getFullName()))
                    .append(",\"email\":").append(jsonString(student.getEmail()))
                    .append(",\"course\":").append(jsonString(student.getCourse()))
                    .append(",\"year\":").append(student.getYear()).append('}');
        }
        json.append("]}");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }

    private void writeMessage(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":" + jsonString(message) + "}");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":" + jsonString(message) + "}");
    }

    private String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.append('"').toString();
    }
}
