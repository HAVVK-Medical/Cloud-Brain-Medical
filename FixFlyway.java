import java.sql.*;

public class FixFlyway {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3307/cloudbrain_medical?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        try (Connection c = DriverManager.getConnection(url, "cloudbrain", "cloudbrain_dev");
             Statement s = c.createStatement()) {
            int rows = s.executeUpdate("DELETE FROM flyway_schema_history WHERE version = '14'");
            System.out.println("Deleted " + rows + " row(s) from flyway_schema_history");
        }
    }
}
