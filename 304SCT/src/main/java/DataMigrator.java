package org.example.utils;

import java.sql.*;

public class DataMigrator {

    // 1. Local SQLite Connection
    private static final String LOCAL_DB_URL = "jdbc:sqlite:sct_data.db";

    // 2. Cloud TiDB Connection
    private static final String CLOUD_DB_URL = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/test?useSSL=true";
    private static final String CLOUD_USER = "3kaEmNyc5ZPLN5i.root";
    private static final String CLOUD_PASS = "Xy2JQNOeLMxiqjFU";

    public static void main(String[] args) {
        System.out.println("Starting data migration...");

        try (Connection localConn = DriverManager.getConnection(LOCAL_DB_URL);
             Connection cloudConn = DriverManager.getConnection(CLOUD_DB_URL, CLOUD_USER, CLOUD_PASS)) {

            // A. Migrate User Data (users table)
            System.out.println("Migrating user data...");
            try (Statement stmt = localConn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

                String insertUser = "INSERT IGNORE INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement pstmt = cloudConn.prepareStatement(insertUser)) {
                    while (rs.next()) {
                        pstmt.setString(1, rs.getString("username"));
                        pstmt.setString(2, rs.getString("password"));
                        pstmt.executeUpdate();
                    }
                }
            }

            // B. Migrate Carbon Logs (carbon_logs table)
            System.out.println("Migrating record data...");
            try (Statement stmt = localConn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM carbon_logs")) {

                // Note: We do not migrate the ID here; let the cloud DB generate new IDs to avoid conflicts.
                String insertLog = "INSERT INTO carbon_logs (username, activity_type, amount, emission, created_at) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = cloudConn.prepareStatement(insertLog)) {
                    while (rs.next()) {
                        pstmt.setString(1, rs.getString("username"));
                        pstmt.setString(2, rs.getString("activity_type"));
                        pstmt.setDouble(3, rs.getDouble("amount"));
                        pstmt.setDouble(4, rs.getDouble("emission"));
                        pstmt.setString(5, rs.getString("created_at")); // Transfer timestamp directly as string
                        pstmt.executeUpdate();
                    }
                }
            }

            System.out.println("✅ Data migration completed! You can now run the main application.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Migration failed. Please check database connection settings.");
        }
    }
}