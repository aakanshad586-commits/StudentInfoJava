package com.studentmanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/student_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String mode = System.getenv("STUDENT_DB_MODE");
        if (mode == null) {
            mode = System.getProperty("STUDENT_DB_MODE", "mysql");
        }
        if ("h2".equalsIgnoreCase(mode)) {
            String url = environmentOrDefault(
                    "STUDENT_DB_URL",
                    "jdbc:h2:file:./data/student_management;DB_CLOSE_ON_EXIT=FALSE");
            return DriverManager.getConnection(url, "sa", "");
        }
        if (!"mysql".equalsIgnoreCase(mode)) {
            throw new SQLException("STUDENT_DB_MODE must be either mysql or h2.");
        }

        String url = environmentOrDefault("STUDENT_DB_URL", DEFAULT_URL);
        String user = environmentOrDefault("STUDENT_DB_USER", "root");
        String password = environmentOrDefault("STUDENT_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }
}
