package cloud.picon.backend.domain.review;

import cloud.picon.backend.domain.expert.summary.ExpertReviewSummaryRefreshIntegrationTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "PICON_REVIEW_MYSQL_PORT", matches = "[1-9][0-9]{0,4}")
class MySqlExpertReviewSummaryRefreshIntegrationTest extends ExpertReviewSummaryRefreshIntegrationTest {

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) {
        ReviewMySqlTestDatabase.configure(properties, "picon_review45_summary_test", true);
    }
}
