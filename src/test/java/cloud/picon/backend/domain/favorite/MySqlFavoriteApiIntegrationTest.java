package cloud.picon.backend.domain.favorite;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.DriverManager;
import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named = MySqlFavoriteApiIntegrationTest.ENV_PREFIX + "_PORT", matches = "[1-9][0-9]{0,4}")
class MySqlFavoriteApiIntegrationTest extends FavoriteApiIntegrationTest {

    static final String ENV_PREFIX = "PICON_FAVORITE_MYSQL";
    private static final String SCHEMA = "picon_favorite49_test";

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) throws SQLException {
        configureMySql(properties, ENV_PREFIX, "picon-favorite-mysql-49-", SCHEMA);
    }

    static void configureMySql(DynamicPropertyRegistry properties, String envPrefix, String hostnamePrefix,
                               String schema) throws SQLException {
        int port = Integer.parseInt(System.getenv(envPrefix + "_PORT"));
        String hostname = System.getenv(envPrefix + "_HOSTNAME");
        if (port < 1024 || port > 65535 || port == 3306 || port == 3307 || port == 33060
                || hostname == null || !hostname.matches(hostnamePrefix + "[a-z0-9]+")) {
            throw new IllegalStateException("Expected a separate loopback port and disposable MySQL hostname");
        }

        String url = "jdbc:mysql://127.0.0.1:" + port + "/" + schema
                + "?sslMode=DISABLED&allowPublicKeyRetrieval=true";
        String username = "favorite_test";
        String password = System.getenv(envPrefix + "_PASSWORD");
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement();
             var server = statement.executeQuery("SELECT @@hostname, DATABASE(), VERSION()")) {
            if (!server.next() || !hostname.equals(server.getString(1)) || !schema.equals(server.getString(2))
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
