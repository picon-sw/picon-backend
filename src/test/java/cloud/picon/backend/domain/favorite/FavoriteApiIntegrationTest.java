package cloud.picon.backend.domain.favorite;

import cloud.picon.backend.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:favorite-api-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER",
        "springdoc.api-docs.enabled=true",
        "app.swagger.username=swagger-test",
        "app.swagger.password=swagger-test"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FavoriteApiIntegrationTest {

    private static final String PATH = "/api/experts/{expertId}/favorite";
    private static final long EXPERT = 10;
    private static final long VIEWER = 20;
    private static final long OTHER_VIEWER = 30;

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider tokens;

    @BeforeEach
    void seedMembersAndExpert() {
        jdbc.update("DELETE FROM favorite");
        jdbc.update("DELETE FROM expert");
        jdbc.update("DELETE FROM member");
        jdbc.update("""
                INSERT INTO member (member_id, nickname, created_at, updated_at)
                VALUES (10, 'Expert', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                       (20, 'Viewer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                       (30, 'Other viewer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO expert (member_id, created_at, updated_at)
                VALUES (10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void addsFavoriteWithoutResponseBodyAndUpdatesExistingExpertDetail() throws Exception {
        detailFor(VIEWER).andExpect(jsonPath("$.result.isFavorite").value(false));

        add(EXPERT, VIEWER).andExpect(status().isNoContent()).andExpect(content().string(""));

        assertThat(favoriteCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM favorite
                WHERE favorite_id > 0 AND member_id = 20 AND expert_member_id = 10 AND created_at IS NOT NULL
                """, Integer.class)).isEqualTo(1);
        detailFor(VIEWER).andExpect(jsonPath("$.result.isFavorite").value(true));
        detailFor(OTHER_VIEWER).andExpect(jsonPath("$.result.isFavorite").value(false));
    }

    @Test
    void requiresJwt() throws Exception {
        mvc.perform(post(PATH, EXPERT))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        assertThat(favoriteCount()).isZero();
    }

    @Test
    void rejectsTokenForMissingMember() throws Exception {
        add(EXPERT, 999).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        assertThat(favoriteCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number", "9223372036854775808"})
    void rejectsInvalidExpertId(String expertId) throws Exception {
        mvc.perform(post(PATH, expertId).header("Authorization", bearer(VIEWER)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(favoriteCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {OTHER_VIEWER, 999})
    void rejectsMissingExpertRow(long expertId) throws Exception {
        add(expertId, VIEWER).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"));
        assertThat(favoriteCount()).isZero();
    }

    @Test
    void acceptsAnExistingExpertWithoutApplyingDetailVisibilityRules() throws Exception {
        jdbc.update("UPDATE member SET nickname = NULL WHERE member_id = ?", EXPERT);
        add(EXPERT, VIEWER).andExpect(status().isNoContent());
        assertThat(favoriteCount()).isEqualTo(1);
    }

    @Test
    void rejectsSelfFavorite() throws Exception {
        add(EXPERT, EXPERT).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(favoriteCount()).isZero();
    }

    @Test
    void returnsConflictForDuplicateFavorite() throws Exception {
        add(EXPERT, VIEWER).andExpect(status().isNoContent());
        add(EXPERT, VIEWER).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_FAVORITED"));
        assertThat(favoriteCount()).isEqualTo(1);
    }

    @Test
    void concurrentRequestsStoreExactlyOneFavorite() throws Exception {
        var start = new CyclicBarrier(2);
        Callable<ResultActions> request = () -> {
            start.await(10, TimeUnit.SECONDS);
            return add(EXPERT, VIEWER);
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            int firstStatus = firstResult.andReturn().getResponse().getStatus();
            int secondStatus = secondResult.andReturn().getResponse().getStatus();
            assertThat(new int[]{firstStatus, secondStatus}).containsExactlyInAnyOrder(204, 409);
            var conflict = firstStatus == 409 ? firstResult : secondResult;
            conflict.andExpect(jsonPath("$.code").value("ALREADY_FAVORITED"));
        }
        assertThat(favoriteCount()).isEqualTo(1);
    }

    @Test
    void documentsBearerAuthNoRequestBodyAndFavoriteResponses() throws Exception {
        String operation = "$.paths['/api/experts/{expertId}/favorite'].post";
        mvc.perform(get("/api/v3/api-docs").with(httpBasic("swagger-test", "swagger-test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(operation + ".parameters.length()").value(1))
                .andExpect(jsonPath(operation + ".requestBody").doesNotExist())
                .andExpect(jsonPath(operation + ".responses['204'].content").doesNotExist())
                .andExpect(jsonPath(operation + ".responses['400'].content['application/json']").exists())
                .andExpect(jsonPath(operation + ".responses['401'].content['application/json']").exists())
                .andExpect(jsonPath(operation + ".responses['404'].content['application/json']").exists())
                .andExpect(jsonPath(operation + ".responses['409'].content['application/json']").exists())
                .andExpect(jsonPath(operation + ".responses['500'].content['application/json']").exists());
    }

    private ResultActions add(long expertId, long viewerId) throws Exception {
        return mvc.perform(post(PATH, expertId).header("Authorization", bearer(viewerId)));
    }

    private ResultActions detailFor(long viewerId) throws Exception {
        return mvc.perform(get("/api/experts/{expertId}", EXPERT).header("Authorization", bearer(viewerId)))
                .andExpect(status().isOk());
    }

    private String bearer(long memberId) {
        return "Bearer " + tokens.createAccessToken(memberId);
    }

    private int favoriteCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM favorite", Integer.class);
    }
}
