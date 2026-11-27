package cloud.picon.backend.domain.review;

import cloud.picon.backend.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "PICON_REVIEW_MYSQL_PORT", matches = "[1-9][0-9]{0,4}")
class MySqlReviewApiIntegrationTest extends ReviewApiIntegrationTest {

    private static final String SCHEMA = "picon_review43_test";

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry properties) {
        ReviewMySqlTestDatabase.configure(properties, SCHEMA, false);
    }

    @Override
    @BeforeEach
    void setUp() {
        recreateSchema("review/mysql-repository-fixture.sql");
    }

    @Test
    void enforcesActualForeignKeyUniqueAndRatingConstraints() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO proposal VALUES (900, 999, 10)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO proposal VALUES (900, 101, 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO reservation VALUES (900, 999, 'COMPLETED')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insert(999, 5, null, null)).isInstanceOf(DataIntegrityViolationException.class);
        for (int rating : new int[]{0, 6}) {
            assertThatThrownBy(() -> insert(301, rating, null, null))
                    .isInstanceOfSatisfying(DataAccessException.class, failure -> {
                        assertThat(failure.getRootCause()).isInstanceOfSatisfying(SQLException.class,
                                sql -> assertThat(sql.getErrorCode()).isEqualTo(3819));
                    });
        }
        insert(301, 1, null, null);
        assertThatThrownBy(() -> insert(301, 5, null, null)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void distinguishesNullAndEmptyBodiesAndStoresTagsAsANativeJsonArray() {
        long tagged = insert(301, 5, "[\"friendly\",\"fast\"]", "");
        long untagged = insert(302, 1, null, null);

        assertThat(untagged).isGreaterThan(tagged);
        assertThat(jdbc.queryForObject("SELECT JSON_TYPE(review_tags) FROM review WHERE review_id = ?",
                String.class, tagged)).isEqualTo("ARRAY");
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(review_tags, '$[0]')) FROM review WHERE review_id = ?",
                String.class, tagged)).isEqualTo("friendly");
        assertThat(jdbc.queryForObject("SELECT review_tags FROM review WHERE review_id = ?",
                String.class, untagged)).isNull();
        assertThat(repository.findBodiesByExpertId(10)).containsExactly(
                new ReviewRepository.ReviewBody(tagged, ""), new ReviewRepository.ReviewBody(untagged, null));
    }

    private long insert(long reservation, int rating, String tags, String body) {
        var now = LocalDateTime.of(2026, 11, 26, 9, 0);
        return repository.insert(new ReviewRepository.NewReview(reservation, rating, tags, body, now, now));
    }
}
