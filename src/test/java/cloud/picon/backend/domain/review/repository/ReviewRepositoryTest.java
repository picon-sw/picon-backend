package cloud.picon.backend.domain.review.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewRepositoryTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 26, 9, 0);
    private static final LocalDateTime UPDATED_AT = CREATED_AT.plusMinutes(1);

    private SingleConnectionDataSource dataSource;
    private ReviewRepository repository;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() throws Exception {
        var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        dataSource = new SingleConnectionDataSource(connection, true);
        new ResourceDatabasePopulator(new ClassPathResource("review/repository-fixture.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        repository = new ReviewRepository(new NamedParameterJdbcTemplate(jdbc));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    void readsRequesterAndExpertFromTheReservationChainWithoutFilteringStatus() {
        assertThat(repository.findReservationContext(301)).contains(
                new ReviewRepository.ReservationContext(301, 201, 101, 1, 10, "CONFIRMED", false));
        assertThat(repository.findReservationContext(999)).isEmpty();
    }

    @Test
    void returnsNoBodiesForAnExpertWithoutReviewsOrAnUnknownExpert() {
        repository.insert(review(301, "Great photos"));

        assertThat(repository.findBodiesByExpertId(30)).isEmpty();
        assertThat(repository.findBodiesByExpertId(999)).isEmpty();
    }

    @Test
    void readsOnlyTheRequestedExpertsReviewsInIdOrder() {
        repository.insert(review(301, "Friendly photographer"));
        repository.insert(review(303, "Another expert"));
        repository.insert(review(302, "Great photos"));

        assertThat(repository.findBodiesByExpertId(10)).containsExactly(
                new ReviewRepository.ReviewBody(401, "Friendly photographer"),
                new ReviewRepository.ReviewBody(403, "Great photos"));
        assertThat(repository.findBodiesByExpertId(20)).containsExactly(
                new ReviewRepository.ReviewBody(402, "Another expert"));
    }

    @Test
    void excludesReservationAndReviewsWhenTheProposalMemberHasNoExpertRow() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE reservation_id = 304", Integer.class))
                .isEqualTo(1);

        assertThat(repository.findReservationContext(304)).isEmpty();
        repository.insert(review(304, "Review for a proposal without an expert"));
        assertThat(repository.findBodiesByExpertId(40)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void storesRatingsAtBothFixtureBoundaries(int rating) {
        repository.insert(new ReviewRepository.NewReview(301, rating, null, null, CREATED_AT, UPDATED_AT));

        assertThat(jdbc.queryForObject("SELECT rating FROM review WHERE review_id = 401", Integer.class))
                .isEqualTo(rating);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void fixtureRejectsRatingsOutsideTheErdRange(int rating) {
        assertThatThrownBy(() -> repository.insert(
                new ReviewRepository.NewReview(301, rating, null, null, CREATED_AT, UPDATED_AT)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(repository.findBodiesByExpertId(10)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t "})
    void preservesMissingEmptyAndWhitespaceOnlyBodies(String content) {
        repository.insert(review(301, content));

        assertThat(repository.findBodiesByExpertId(10))
                .containsExactly(new ReviewRepository.ReviewBody(401, content));
    }

    @Test
    void bindsNonNullTagTextUsingH2JsonStringCoercion() {
        // H2 wraps VARCHAR input in a JSON string. MySQL JSON parsing is a separate check.
        repository.insert(new ReviewRepository.NewReview(301, 5, "[]", null, CREATED_AT, UPDATED_AT));

        assertThat(jdbc.queryForObject("SELECT review_tags FROM review WHERE review_id = 401", String.class))
                .isEqualTo("\"[]\"");
    }

    @Test
    void persistsReviewFieldsAndReportsAnExistingReview() {
        repository.insert(review(301, "촬영이 편했어요."));

        assertThat(repository.findReservationContext(301)).contains(
                new ReviewRepository.ReservationContext(301, 201, 101, 1, 10, "CONFIRMED", true));
        assertThat(jdbc.queryForObject("SELECT rating FROM review WHERE review_id = 401", Integer.class))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT review_tags FROM review WHERE review_id = 401", String.class))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT review_content FROM review WHERE review_id = 401", String.class))
                .isEqualTo("촬영이 편했어요.");
        assertThat(jdbc.queryForObject("SELECT created_at FROM review WHERE review_id = 401", LocalDateTime.class))
                .isEqualTo(CREATED_AT);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM review WHERE review_id = 401", LocalDateTime.class))
                .isEqualTo(UPDATED_AT);
    }

    @Test
    void fixtureRejectsDuplicateReservationsAndPreservesTheOriginalReview() {
        repository.insert(review(301, "Original"));

        assertThatThrownBy(() -> repository.insert(review(301, "Duplicate")))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(repository.findBodiesByExpertId(10))
                .containsExactly(new ReviewRepository.ReviewBody(401, "Original"));
    }

    @Test
    void fixtureRejectsAReviewWithoutAReservation() {
        assertThatThrownBy(() -> repository.insert(review(999, "Missing reservation")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ReviewRepository.NewReview review(long reservationId, String content) {
        return new ReviewRepository.NewReview(reservationId, 5, null, content, CREATED_AT, UPDATED_AT);
    }
}
