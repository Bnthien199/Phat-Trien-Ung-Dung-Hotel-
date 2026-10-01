package connectDB;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectDB {

    private static final Properties DB_PROPERTIES = loadProperties();
    private static final String DEFAULT_URL =
            "jdbc:sqlserver://localhost:1433;databaseName=QuanLyKhachSan;encrypt=true;trustServerCertificate=true;";
    private static final String URL = DB_PROPERTIES.getProperty("db.url", DEFAULT_URL);
    private static final String USER =
            DB_PROPERTIES.getProperty("db.user", "sy");
    private static final String PASSWORD =
            DB_PROPERTIES.getProperty("db.password", "123");

    private static final ConnectDB INSTANCE = new ConnectDB();

    public static ConnectDB getInstance() {
        return INSTANCE;
    }

    public void connect() throws SQLException {
        loadDriver();
        DriverManager.setLoginTimeout(5);

        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to database successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            throw new SQLException("Khong the ket noi database. Kiem tra SQL Server, tai khoan va cau hinh ket noi.", e);
        }
    }

    public void disconnect() {
        // Each DAO call manages its own Connection via try-with-resources.
    }

    public static Connection getConnection() {
        try {
            loadDriver();
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Khong the tao ket noi database.", e);
        }
    }

    public static void main(String[] args) throws SQLException {
        getInstance().connect();
    }

    private static void loadDriver() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Khong tim thay JDBC Driver SQL Server.", e);
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = ConnectDB.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Khong the doc file db.properties: " + e.getMessage());
        }

        return properties;
    }
}
