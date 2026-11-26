package cloud.picon.backend.domain.review;

import cloud.picon.backend.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:review-api-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER",
        "spring.jpa.hibernate.ddl-auto=none"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ReviewApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @MockitoSpyBean ReviewRepository repository;

    @BeforeEach
    void setUp() {
        recreateSchema("review/repository-fixture.sql");
        jdbc.execute("CREATE TABLE member (member_id BIGINT PRIMARY KEY, nickname VARCHAR(20))");
        jdbc.update("INSERT INTO member VALUES (10, 'Expert A'), (20, 'Expert B'), (30, 'Expert C')");
        jdbc.execute("ALTER TABLE expert ADD review_count INT NOT NULL DEFAULT 0");
        jdbc.execute("ALTER TABLE expert ADD avg_rating DECIMAL(2,1) NOT NULL DEFAULT 0.0");
        // Emulate MySQL's JSON text round trip. Native H2 JSON binding is tested separately.
        jdbc.execute("ALTER TABLE review ALTER COLUMN review_tags VARCHAR");
    }

    void recreateSchema(String fixture) {
        for (String table : new String[]{"review", "reservation", "proposal", "`request`", "expert", "member"}) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        new ResourceDatabasePopulator(new ClassPathResource(fixture)).execute(dataSource);
    }

    @Test
    void registersReviewReturnsGeneratedIdAndUpdatesExpertAggregate() throws Exception {
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json")
                        .content("""
                                {"rating":4,"reviewTags":["friendly"],"reviewContent":"Great photos"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.result.reviewId").value(401))
                .andExpect(jsonPath("$.result.reviewTags[0]").value("friendly"));

        jdbc.update("UPDATE reservation SET status = 'COMPLETED' WHERE reservation_id = 301");
        mvc.perform(post("/api/reservations/301/reviews").with(jwt().jwt(token -> token.subject("1")))
                        .contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT review_count FROM expert WHERE member_id = 10", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT avg_rating FROM expert WHERE member_id = 10", BigDecimal.class))
                .isEqualByComparingTo("4.5");
        mvc.perform(get("/api/experts/10/reviews").with(jwt().jwt(token -> token.subject("2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[?(@.reviewId == 401)].reviewTags[0]").value("friendly"));
    }

    @Test
    void rejectsUnauthenticatedWrongOwnerAndIncompleteReservation() throws Exception {
        mvc.perform(post("/api/reservations/302/reviews").contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("1")))
                        .contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/reservations/301/reviews").with(jwt().jwt(token -> token.subject("1")))
                        .contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS"));
        mvc.perform(post("/api/reservations/999/reviews").with(jwt().jwt(token -> token.subject("1")))
                        .contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"rating\":0}", "{\"rating\":6}", "{\"rating\":4.5}",
            "{\"rating\":\"5\"}", "{\"rating\":5,\"reviewTags\":{}}", "{\"rating\":5,\"reviewTags\":\"tag\"}"})
    void rejectsInvalidRatingAndNonArrayTags(String body) throws Exception {
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "한", "😀"})
    void countsBodyCodePointsAndRejectsContentAboveTheLimit(String character) throws Exception {
        String atLimit = character.repeat(2_000);
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5,\"reviewContent\":\""
                                + atLimit + character + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review", Integer.class)).isZero();

        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5,\"reviewContent\":\"" + atLimit + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.reviewContent").value(atLimit));
        assertThat(jdbc.queryForObject("SELECT review_content FROM review", String.class)).isEqualTo(atLimit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\""})
    void acceptsNullAndEmptyBodies(String json) throws Exception {
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5,\"reviewContent\":" + json + "}"))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT review_content FROM review", String.class))
                .isEqualTo("null".equals(json) ? null : "");
    }

    @Test
    void limitsSerializedTagBytesWithoutRestrictingArrayElements() throws Exception {
        // Brackets and quotes take four bytes, and each emoji takes four UTF-8 bytes.
        String atLimit = "😀".repeat(4_095);
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5,\"reviewTags\":[\"" + atLimit + "a\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review", Integer.class)).isZero();

        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5,\"reviewTags\":[\"" + atLimit + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.reviewTags[0]").value(atLimit));
    }

    @Test
    void concurrentDuplicatesProduceOneCreatedAndOneConflict() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var requests = List.of(1, 2).stream().map(ignored -> executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                                .contentType("application/json").content("{\"rating\":5}"))
                        .andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();

            assertThat(List.of(requests.get(0).get(10, TimeUnit.SECONDS),
                    requests.get(1).get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT review_count FROM expert WHERE member_id = 10", Integer.class))
                .isEqualTo(1);
        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
    }

    @Test
    void concurrentRegistrationsForOneExpertKeepBothReviewsInTheAggregate() throws Exception {
        jdbc.update("UPDATE reservation SET status = 'COMPLETED' WHERE reservation_id = 301");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var requests = List.of(1, 2).stream().map(memberId -> executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/reservations/{reservationId}/reviews", 300 + memberId)
                                .with(jwt().jwt(token -> token.subject(memberId.toString())))
                                .contentType("application/json").content("{\"rating\":" + (3 + memberId) + "}"))
                        .andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            assertThat(List.of(requests.get(0).get(10, TimeUnit.SECONDS), requests.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactly(201, 201);
        }
        assertThat(jdbc.queryForObject("SELECT review_count FROM expert WHERE member_id = 10", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT avg_rating FROM expert WHERE member_id = 10", BigDecimal.class))
                .isEqualByComparingTo("4.5");
    }

    @Test
    void rollsBackReviewWhenAggregationFails() throws Exception {
        doThrow(new IllegalStateException("Test aggregation failure")).when(repository).refreshExpertRating(10);

        mvc.perform(post("/api/reservations/302/reviews").with(jwt().jwt(token -> token.subject("2")))
                        .contentType("application/json").content("{\"rating\":5}"))
                .andExpect(status().isInternalServerError());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT review_count FROM expert WHERE member_id = 10", Integer.class)).isZero();
    }

    @Test
    void listsOnlyTargetExpertInNewestOrderWithPagingAndHandlesEmptyAndMissingExperts() throws Exception {
        jdbc.update("""
                INSERT INTO review (review_id,reservation_id,rating,review_tags,review_content,created_at,updated_at)
                VALUES (410,301,4,'["older"]','Older','2026-11-25 00:00:00','2026-11-25 00:00:00'),
                       (409,302,5,'["newer"]','Newer','2026-11-26 00:00:00','2026-11-26 00:00:00'),
                       (411,303,3,NULL,'Other','2026-11-26 00:00:00','2026-11-26 00:00:00')
                """);

        mvc.perform(get("/api/experts/10/reviews").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.content[0].reviewId").value(409))
                .andExpect(jsonPath("$.result.content[0].reviewTags[0]").value("newer"))
                .andExpect(jsonPath("$.result.content[1].reviewId").value(410));
        mvc.perform(get("/api/experts/10/reviews?page=1&size=1").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content[0].reviewId").value(410))
                .andExpect(jsonPath("$.result.last").value(true));
        mvc.perform(get("/api/experts/30/reviews").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content").isEmpty());
        mvc.perform(get("/api/experts/999/reviews").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"));
        mvc.perform(get("/api/experts/10/reviews?size=101").with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/experts/10/reviews")).andExpect(status().isUnauthorized());
    }
}
