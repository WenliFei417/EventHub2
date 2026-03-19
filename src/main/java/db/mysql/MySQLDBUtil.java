package db.mysql;

public class MySQLDBUtil {
    // Render 提供的 DATABASE_URL 格式：
    // postgres://用户名:密码@主机:5432/数据库名
    // 我们需要解析成 JDBC 格式

    public static final String USERNAME;
    public static final String PASSWORD;
    public static final String URL;

    static {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgres")) {
            // 解析 Render 提供的 DATABASE_URL
            // 格式: postgres://user:pass@host:port/dbname
            try {
                java.net.URI uri = new java.net.URI(databaseUrl);
                String host = uri.getHost();
                int port = uri.getPort();
                String dbName = uri.getPath().substring(1); // 去掉开头的 /
                String[] userInfo = uri.getUserInfo().split(":");

                USERNAME = userInfo[0];
                PASSWORD = userInfo[1];
                URL = "jdbc:postgresql://" + host + ":" + port + "/" + dbName
                        + "?sslmode=require";
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse DATABASE_URL: " + databaseUrl, e);
            }
        } else {
            // 本地开发用
            USERNAME = System.getenv().getOrDefault("DB_USER", "root");
            PASSWORD = System.getenv().getOrDefault("DB_PASS", "");
            URL = "jdbc:postgresql://127.0.0.1:5432/wlproject";
        }
    }
}