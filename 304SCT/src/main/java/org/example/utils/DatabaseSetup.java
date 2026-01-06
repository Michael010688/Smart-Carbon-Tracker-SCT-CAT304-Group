package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    // 🔴 1. Configure TiDB Connection Info
    // Please go to TiDB Console -> Connect -> Copy that URL (Note: keep the part after ?sslMode=...)
    // Typical format is: jdbc:mysql://gateway01.....:4000/test?sslMode=VERIFY_IDENTITY...
    private static final String DB_URL = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/test?sslMode=VERIFY_IDENTITY&useSSL=true&allowPublicKeyRetrieval=true";

    // 🔴 2. Enter your TiDB Username (e.g.: 4kD8s.root)
    private static final String USER = "3kaEmNyc5ZPLN5i.root";

    // 🔴 3. Enter your TiDB Password
    private static final String PASSWORD = "Xy2JQNOeLMxiqjFU";

    public static void main(String[] args) {
        initializeDatabase();
    }

    // ✅ New: Provide a public method to get the connection, for other classes (like SCTMobileApp) to call
    public static Connection getDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        // Use try-with-resources to automatically close the connection
        try (Connection conn = getDatabaseConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Connecting to TiDB Cloud...");

            // 🟡 3. Modify table creation statement (Adapted for MySQL/TiDB syntax)
            // SQLite uses AUTOINCREMENT, MySQL uses AUTO_INCREMENT
            // SQLite uses REAL, MySQL suggests DOUBLE
            String sql = "CREATE TABLE IF NOT EXISTS carbon_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "activity_type VARCHAR(255), " +
                    "amount DOUBLE, " +
                    "emission DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            stmt.executeUpdate(sql);

            System.out.println("✅ Success! Connected to Cloud and table 'carbon_logs' checked.");

        } catch (SQLException e) {
            System.out.println("❌ Connection Failed!");
            System.out.println("Error Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ... Existing helper methods ...

    /**
     * Update user's password
     * @param username The currently logged-in username
     * @param newPassword The new password the user wants to set
     * @return true if update successful, false if failed
     */
    public static boolean updateUserPassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = getDatabaseConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // If affected rows > 0, the update was successful

        } catch (SQLException e) {
            System.out.println("❌ Update Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's nickname
     */
    public static boolean updateUserNickname(String username, String newNickname) {
        // SQL statement: Update nickname column
        String sql = "UPDATE users SET nickname = ? WHERE username = ?";

        try (Connection conn = getDatabaseConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newNickname);
            pstmt.setString(2, username);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.out.println("❌ Nickname Update Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get user's nickname (for display)
     * If no nickname exists, return the username
     */
    public static String getNickname(String username) {
        String sql = "SELECT nickname FROM users WHERE username = ?";
        String nickname = username; // Default to displaying username

        try (Connection conn = getDatabaseConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            java.sql.ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbNickname = rs.getString("nickname");
                // If the database has a nickname and it is not empty, use the nickname
                if (dbNickname != null && !dbNickname.isEmpty()) {
                    nickname = dbNickname;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nickname;
    }
}