package cloud.picon.backend.domain.review;

import cloud.picon.backend.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:review-schema-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MEMBER",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ReviewSchemaIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 9, 0);

    @Autowired ReviewRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void hibernateCreatesAllFourIdentityColumns() {
        assertThat(jdbc.queryForList("""
                SELECT LOWER(COLUMN_NAME)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC' AND IS_IDENTITY = 'YES'
                  AND LOWER(TABLE_NAME) IN ('request', 'proposal', 'reservation', 'review')
                """, String.class))
                .containsExactlyInAnyOrder("request_id", "proposal_id", "reservation_id", "review_id");
    }

    @Test
    void generatedSchemaEnforcesForeignKeysUniqueConstraintsAndRatingCheck() {
        seedMembers();
        assertThatThrownBy(() -> insertRequest(101, 999)).isInstanceOf(DataIntegrityViolationException.class);
        insertRequest(101, 1);
        assertThatThrownBy(() -> jdbc.update("UPDATE `request` SET region_code = 999 WHERE request_id = 101"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE `request` SET photo_style_id = 999 WHERE request_id = 101"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertProposal(201, 999, 10)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertProposal(201, 101, 1)).isInstanceOf(DataIntegrityViolationException.class);
        insertProposal(201, 101, 10);
        assertThatThrownBy(() -> insertProposal(202, 101, 10)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertReservation(301, 999)).isInstanceOf(DataIntegrityViolationException.class);
        insertReservation(301, 201);
        assertThatThrownBy(() -> insertReservation(302, 201)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertReview(999, 5)).isInstanceOf(DataIntegrityViolationException.class);
        for (int rating : new int[]{0, 6}) {
            assertThatThrownBy(() -> insertReview(301, rating))
                    .isInstanceOfSatisfying(DataAccessException.class, failure -> {
                        assertThat(failure.getRootCause()).isInstanceOfSatisfying(SQLException.class,
                                sql -> assertThat(sql.getErrorCode()).isEqualTo(ratingCheckErrorCode()));
                    });
        }
        assertThat(insertReview(301, 1)).isPositive();
        assertThatThrownBy(() -> insertReview(301, 5)).isInstanceOf(DuplicateKeyException.class);
    }

    int ratingCheckErrorCode() {
        return 23513;
    }

    @Test
    void registersAndListsReviewsWithDuplicateProtectionAndExpertRatingOnGeneratedSchema() throws Exception {
        seedMembers();
        for (int number = 1; number <= 2; number++) {
            insertRequest(100 + number, 1);
            insertProposal(200 + number, 100 + number, 10);
            insertReservation(300 + number, 200 + number);
        }
        assertThat(jdbc.queryForObject("SELECT status FROM `request` WHERE request_id = 101", String.class))
                .isEqualTo("REQUESTED");
        assertThat(jdbc.queryForObject("SELECT status FROM reservation WHERE reservation_id = 301", String.class))
                .isEqualTo("CONFIRMED");
        jdbc.update("UPDATE reservation SET status = 'COMPLETED'");

        register(301, 3).andExpect(status().isCreated());
        register(302, 5).andExpect(status().isCreated());
        register(301, 4).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));

        mvc.perform(get("/api/experts/10/reviews").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].rating").value(5))
                .andExpect(jsonPath("$.result.content[1].rating").value(3));
        assertThat(jdbc.queryForObject("SELECT review_count FROM expert WHERE member_id = 10", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT avg_rating FROM expert WHERE member_id = 10", BigDecimal.class))
                .isEqualByComparingTo("4.0");
    }

    private void seedMembers() {
        jdbc.update("INSERT INTO member (member_id, nickname, created_at, updated_at) VALUES (1, 'Requester', ?, ?)", NOW, NOW);
        jdbc.update("INSERT INTO member (member_id, nickname, created_at, updated_at) VALUES (10, 'Expert', ?, ?)", NOW, NOW);
        jdbc.update("INSERT INTO expert (member_id, created_at, updated_at) VALUES (10, ?, ?)", NOW, NOW);
    }

    private void insertRequest(long id, long memberId) {
        jdbc.update("""
                INSERT INTO `request` (request_id, member_id, service_type, title, created_at, updated_at)
                VALUES (?, ?, 'SHOOTING', 'Portrait', ?, ?)
                """, id, memberId, NOW, NOW);
    }

    private void insertProposal(long id, long requestId, long expertId) {
        jdbc.update("""
                INSERT INTO proposal (proposal_id, request_id, member_id, price, created_at, updated_at)
                VALUES (?, ?, ?, 10000, ?, ?)
                """, id, requestId, expertId, NOW, NOW);
    }

    private void insertReservation(long id, long proposalId) {
        jdbc.update("""
                INSERT INTO reservation (reservation_id, proposal_id, reservation_date, reservation_time, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, proposalId, NOW.toLocalDate(), NOW.toLocalTime(), NOW, NOW);
    }

    private long insertReview(long reservationId, int rating) {
        return repository.insert(new ReviewRepository.NewReview(reservationId, rating, null, null, NOW, NOW));
    }

    private ResultActions register(long reservationId, int rating) throws Exception {
        return mvc.perform(post("/api/reservations/{reservationId}/reviews", reservationId)
                .with(jwt().jwt(token -> token.subject("1")))
                .contentType("application/json").content("{\"rating\":" + rating + "}"));
    }
}
