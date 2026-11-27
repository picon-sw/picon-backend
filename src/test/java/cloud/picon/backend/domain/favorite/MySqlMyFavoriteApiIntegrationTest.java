package cloud.picon.backend.domain.favorite;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named = MySqlMyFavoriteApiIntegrationTest.ENV_PREFIX + "_PORT", matches = "[1-9][0-9]{0,4}")
class MySqlMyFavoriteApiIntegrationTest extends MyFavoriteApiIntegrationTest {

    static final String ENV_PREFIX = "PICON_MY_FAVORITES_MYSQL";

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) throws SQLException {
        MySqlFavoriteApiIntegrationTest.configureMySql(
                properties, ENV_PREFIX, "picon-favorite-mysql-57-", "picon_favorite57_test");
    }
}
