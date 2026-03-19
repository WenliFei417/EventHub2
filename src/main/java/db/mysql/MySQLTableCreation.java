package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLTableCreation {
    // Run this as a Java application to reset db schema.
    public static void main(String[] args) {
        // 使用 try-with-resources 自动关闭连接（不需要手动 close）
        System.out.println("Connecting with URL=" + MySQLDBUtil.URL + " user=" + MySQLDBUtil.USERNAME);
        try (Connection conn = DriverManager.getConnection(MySQLDBUtil.URL, MySQLDBUtil.USERNAME, MySQLDBUtil.PASSWORD)) {

            System.out.println("Connecting to " + MySQLDBUtil.URL);

            if (conn == null) {
                System.err.println("❌ Failed to connect to database.");
                return;
            }

            System.out.println("✅ Connected successfully!");
            // 在这里你可以添加建表、删除表、插入初始数据等 SQL 操作。例如：
            // Statement stmt = conn.createStatement();
            // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(50))");


            // Step 2: Drop tables in case they exist.
            Statement stmt = conn.createStatement();
            String sql = "DROP TABLE IF EXISTS categories";
            stmt.executeUpdate(sql);

            sql = "DROP TABLE IF EXISTS history";
            stmt.executeUpdate(sql);

            sql = "DROP TABLE IF EXISTS items";
            stmt.executeUpdate(sql);

            sql = "DROP TABLE IF EXISTS users";
            stmt.executeUpdate(sql);

            // Step 3: Create new tables
            sql = "CREATE TABLE items ("
                    + "item_id VARCHAR(255) NOT NULL,"
                    + "name VARCHAR(255),"
                    + "rating FLOAT,"
                    + "address VARCHAR(255),"
                    + "image_url VARCHAR(255),"
                    + "url VARCHAR(255),"
                    + "distance FLOAT,"
                    + "PRIMARY KEY (item_id))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE categories ("
                    + "item_id VARCHAR(255) NOT NULL,"
                    + "category VARCHAR(255) NOT NULL,"
                    + "PRIMARY KEY (item_id, category),"
                    + "FOREIGN KEY (item_id) REFERENCES items(item_id))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE users ("
                    + "user_id VARCHAR(255) NOT NULL,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "first_name VARCHAR(255),"
                    + "last_name VARCHAR(255),"
                    + "PRIMARY KEY (user_id))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE history ("
                    + "user_id VARCHAR(255) NOT NULL,"
                    + "item_id VARCHAR(255) NOT NULL,"
                    + "last_favor_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (user_id, item_id),"
                    + "FOREIGN KEY (item_id) REFERENCES items(item_id),"
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id))";
            stmt.executeUpdate(sql);

            // Step 4: insert data (add a fake user since we don't have a register function)
//            sql = "INSERT INTO users (user_id, password, first_name, last_name) VALUES ("
//                    + "'1111', '3229c1097c00d497a0fd282d586be050', 'John', 'Smith')";
//            System.out.println("Executing query: " + sql);
//            stmt.executeUpdate(sql);

            System.out.println("✅ Import is done successfully.");

        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}