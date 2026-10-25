package com.studentmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    public List<Student> findAll() throws SQLException {
        return inTransaction(connection -> {
            String sql = "SELECT id, full_name, email, course, academic_year FROM students ORDER BY id DESC";
            List<Student> students = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(new Student(
                            resultSet.getLong("id"),
                            resultSet.getString("full_name"),
                            resultSet.getString("email"),
                            resultSet.getString("course"),
                            resultSet.getInt("academic_year")));
                }
            }
            return students;
        });
    }

    public Student create(Student student) throws SQLException {
        return inTransaction(connection -> {
            String sql = "INSERT INTO students (full_name, email, course, academic_year) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindStudent(statement, student);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("The student record was not inserted.");
                }
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("The database did not return the new student ID.");
                    }
                    return new Student(keys.getLong(1), student.getFullName(), student.getEmail(),
                            student.getCourse(), student.getYear());
                }
            }
        });
    }

    public boolean update(Student student) throws SQLException {
        return inTransaction(connection -> {
            String sql = "UPDATE students SET full_name = ?, email = ?, course = ?, academic_year = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindStudent(statement, student);
                statement.setLong(5, student.getId());
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean delete(long id) throws SQLException {
        return inTransaction(connection -> {
            String sql = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                return statement.executeUpdate() == 1;
            }
        });
    }

    private void bindStudent(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getFullName());
        statement.setString(2, student.getEmail());
        statement.setString(3, student.getCourse());
        statement.setInt(4, student.getYear());
    }

    private <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    public static final class Student {
        private final long id;
        private final String fullName;
        private final String email;
        private final String course;
        private final int year;

        public Student(long id, String fullName, String email, String course, int year) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.course = course;
            this.year = year;
        }

        public long getId() {
            return id;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getCourse() {
            return course;
        }

        public int getYear() {
            return year;
        }
    }
}
