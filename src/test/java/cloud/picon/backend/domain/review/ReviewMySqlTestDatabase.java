package cloud.picon.backend.domain.review;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.sql.DriverManager;
import java.sql.SQLException;

final class ReviewMySqlTestDatabase {

    private ReviewMySqlTestDatabase() {
    }

    static void configure(DynamicPropertyRegistry properties, String schema, boolean requireEmpty) {
        int port = Integer.parseInt(System.getenv("PICON_REVIEW_MYSQL_PORT"));
        if (port < 1024 || port > 65535 || port == 3306 || port == 3307 || port == 33060) {
            throw new IllegalStateException("Use a separate local test port");
        }
        String hostname = System.getenv("PICON_REVIEW_MYSQL_HOSTNAME");
        if (hostname == null || !hostname.matches("picon-review-mysql-43-[a-z0-9]+")) {
            throw new IllegalStateException("Expected the hostname of the disposable MySQL test container");
        }
        String url = "jdbc:mysql://127.0.0.1:" + port + "/" + schema
                + "?sslMode=DISABLED&allowPublicKeyRetrieval=true";
        String username = "review_test";
        String password = System.getenv("PICON_REVIEW_MYSQL_PASSWORD");

        // Verify the server and schema before Hibernate can run DDL.
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement();
             var server = statement.executeQuery("SELECT @@hostname, DATABASE(), VERSION()")) {
            if (!server.next() || !hostname.equals(server.getString(1)) || !schema.equals(server.getString(2))
                    || !server.getString(3).startsWith("8.4.")) {
                throw new IllegalStateException("Expected the disposable MySQL 8.4 server and schema");
            }
            if (requireEmpty) {
                try (var tables = statement.executeQuery("SHOW TABLES")) {
                    if (tables.next()) {
                        throw new IllegalStateException("Hibernate schema verification requires an empty database");
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify the isolated MySQL test database", e);
        }

        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("spring.datasource.username", () -> username);
        properties.add("spring.datasource.password", () -> password);
    }
}
