package cloud.picon.backend.domain.favorite;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.DriverManager;
import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named = "PICON_FAVORITE_MYSQL_PORT", matches = "[1-9][0-9]{0,4}")
class MySqlFavoriteApiIntegrationTest extends FavoriteApiIntegrationTest {

    private static final String SCHEMA = "picon_favorite49_test";

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) throws SQLException {
        int port = Integer.parseInt(System.getenv("PICON_FAVORITE_MYSQL_PORT"));
        String hostname = System.getenv("PICON_FAVORITE_MYSQL_HOSTNAME");
        if (port < 1024 || port > 65535 || port == 3306 || port == 3307 || port == 33060
                || hostname == null || !hostname.matches("picon-favorite-mysql-49-[a-z0-9]+")) {
            throw new IllegalStateException("Expected a separate loopback port and disposable MySQL hostname");
        }

        String url = "jdbc:mysql://127.0.0.1:" + port + "/" + SCHEMA
                + "?sslMode=DISABLED&allowPublicKeyRetrieval=true";
        String username = "favorite_test";
        String password = System.getenv("PICON_FAVORITE_MYSQL_PASSWORD");
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement();
             var server = statement.executeQuery("SELECT @@hostname, DATABASE(), VERSION()")) {
            if (!server.next() || !hostname.equals(server.getString(1)) || !SCHEMA.equals(server.getString(2))
                    || !server.getString(3).startsWith("8.4.")) {
                throw new IllegalStateException("Expected the disposable MySQL 8.4 instance and favorite test schema");
            }
        }

        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("spring.datasource.username", () -> username);
        properties.add("spring.datasource.password", () -> password);
    }
}
