package cloud.picon.backend.domain.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "PICON_REVIEW_MYSQL_PORT", matches = "[1-9][0-9]{0,4}")
class MySqlReviewSchemaIntegrationTest extends ReviewSchemaIntegrationTest {

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) {
        ReviewMySqlTestDatabase.configure(properties, "picon_review43_schema_test", true);
    }

    @Override
    @Test
    void hibernateCreatesAllFourIdentityColumns() {
        assertThat(jdbc.queryForList("""
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND EXTRA = 'auto_increment' AND COLUMN_KEY = 'PRI'
                  AND TABLE_NAME IN ('request', 'proposal', 'reservation', 'review')
                """, String.class))
                .containsExactlyInAnyOrder("request_id", "proposal_id", "reservation_id", "review_id");
    }

    @Override
    int ratingCheckErrorCode() {
        return 3819;
    }

    @Test
    void hibernateCreatesNativeJsonAndDatetimeColumns() {
        assertThat(jdbc.queryForList("""
                SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME, ':', COLUMN_TYPE)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN ('request', 'proposal', 'reservation', 'review')
                  AND COLUMN_NAME IN ('available_time', 'review_tags', 'created_at', 'updated_at')
                """, String.class))
                .containsExactlyInAnyOrder(
                        "request.created_at:datetime", "request.updated_at:datetime",
                        "proposal.created_at:datetime", "proposal.updated_at:datetime", "proposal.available_time:json",
                        "reservation.created_at:datetime", "reservation.updated_at:datetime",
                        "review.created_at:datetime", "review.updated_at:datetime", "review.review_tags:json");
        assertThat(jdbc.queryForObject("""
                SELECT REFERENCED_TABLE_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'proposal' AND COLUMN_NAME = 'member_id'
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, String.class)).isEqualTo("expert");
    }
}
