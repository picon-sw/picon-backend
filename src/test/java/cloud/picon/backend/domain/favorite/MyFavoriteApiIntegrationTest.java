package cloud.picon.backend.domain.favorite;

import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:my-favorite-test;MODE=MySQL;NON_KEYWORDS=MEMBER",
        "springdoc.api-docs.enabled=true",
        "app.swagger.username=swagger-test",
        "app.swagger.password=swagger-test"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MyFavoriteApiIntegrationTest {

    private static final String PATH = "/api/members/me/favorites";
    private static final long MEMBER = 1;
    private static final long OTHER = 2;
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider tokens;
    @Autowired JsonMapper mapper;
    @MockitoBean S3ImageUrlProvider imageUrlProvider;

    @BeforeEach
    void setUp() {
        for (String table : List.of("favorite", "expert_specialty", "expert", "member")) {
            jdbc.update("DELETE FROM " + table);
        }
        seedMember(MEMBER, "Viewer");
        seedMember(OTHER, "Other viewer");
    }

    @Test
    void listsOnlyAuthenticatedMembersFavoritesWithCommonResponseAndStoredProfileData() throws Exception {
        seedExpert(10, "Studio");
        seedExpert(11, "Other studio");
        jdbc.update("UPDATE member SET profile_image = ?, introduction = ? WHERE member_id = ?",
                "profiles/studio.jpg", "Portrait photographer", 10);
        price(10, "SHOOTING", 80000);
        price(10, "EDITING", 30000);
        mvc.perform(post("/api/experts/10/favorite").header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk());
        favorite(OTHER, 11, TIME);
        when(imageUrlProvider.getImageUrl("profiles/studio.jpg")).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "https://images.example.invalid/studio.jpg";
        });

        mvc.perform(get(PATH).header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].expertId").value(10))
                .andExpect(jsonPath("$.result.content[0].nickname").value("Studio"))
                .andExpect(jsonPath("$.result.content[0].profileImage").value("https://images.example.invalid/studio.jpg"))
                .andExpect(jsonPath("$.result.content[0].introduction").value("Portrait photographer"))
                .andExpect(jsonPath("$.result.content[0].startingPrice").value(30000))
                .andExpect(jsonPath("$.result.content[0].rating").value(4.8))
                .andExpect(jsonPath("$.result.content[0].reviewCount").value(12))
                .andExpect(jsonPath("$.result.content[0].isFavorite").value(true))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
        mvc.perform(get(PATH).header("Authorization", bearer(OTHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].expertId").value(11));
    }

    @Test
    void ordersByFavoriteCreationThenIdAndHandlesLastAndOutOfRangePages() throws Exception {
        for (int id = 10; id <= 12; id++) {
            seedExpert(id, "Studio " + id);
        }
        favorite(MEMBER, 10, TIME.plusDays(1));
        favorite(MEMBER, 11, TIME);
        favorite(MEMBER, 12, TIME);

        mvc.perform(get(PATH).param("size", "2").header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].expertId").value(10))
                .andExpect(jsonPath("$.result.content[1].expertId").value(12))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(false));
        mvc.perform(get(PATH).param("page", "1").param("size", "2").header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].expertId").value(11))
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
        mvc.perform(get(PATH).param("page", "2147483647").param("size", "100").header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.page").value(Integer.MAX_VALUE))
                .andExpect(jsonPath("$.result.size").value(100))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
    }

    @Test
    void returnsEmptyPageWithRequestedMetadataWhenMemberHasNoFavorites() throws Exception {
        mvc.perform(get(PATH).param("page", "3").param("size", "5").header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.page").value(3))
                .andExpect(jsonPath("$.result.size").value(5))
                .andExpect(jsonPath("$.result.totalElements").value(0))
                .andExpect(jsonPath("$.result.totalPages").value(0))
                .andExpect(jsonPath("$.result.last").value(true));
    }

    @Test
    void appliesVisibilityToContentAndCountWithoutDeletingHiddenFavorites() throws Exception {
        seedExpert(10, "  Visible\u2009");
        seedExpert(11, null);
        seedExpert(12, "");
        seedExpert(13, "   ");
        seedExpert(14, "\u00a0\u2009");
        for (int id = 10; id <= 14; id++) {
            favorite(MEMBER, id, TIME.plusDays(id));
        }

        mvc.perform(get(PATH).param("size", "1").header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].expertId").value(10))
                .andExpect(jsonPath("$.result.content[0].nickname").value("  Visible\u2009"))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM favorite WHERE member_id = 1", Integer.class))
                .isEqualTo(5);
    }

    @Test
    void returnsNullableFieldsAndOnlyUsesValidRegisteredPrices() throws Exception {
        seedExpert(10, "Valid price");
        seedExpert(11, "No price");
        seedExpert(12, "Invalid price");
        price(10, "SHOOTING", -1);
        price(10, "EDITING", 0);
        price(12, "SHOOTING", -10);
        for (int id = 10; id <= 12; id++) {
            favorite(MEMBER, id, TIME.minusDays(id));
        }

        mvc.perform(get(PATH).header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].startingPrice").value(0))
                .andExpect(jsonPath("$.result.content[1].profileImage").value((Object) null))
                .andExpect(jsonPath("$.result.content[1].introduction").value((Object) null))
                .andExpect(jsonPath("$.result.content[1].startingPrice").value((Object) null))
                .andExpect(jsonPath("$.result.content[2].startingPrice").value((Object) null))
                .andExpect(jsonPath("$.result.totalElements").value(3));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void reusesImageKeySignaturesWithinPageAndTreatsBlankKeysAsNull() throws Exception {
        for (int id = 10; id <= 12; id++) {
            seedExpert(id, "Studio " + id);
            favorite(MEMBER, id, TIME.minusDays(id));
        }
        jdbc.update("UPDATE member SET profile_image = 'profiles/shared.jpg' WHERE member_id IN (10, 11)");
        jdbc.update("UPDATE member SET profile_image = '   ' WHERE member_id = 12");
        when(imageUrlProvider.getImageUrl("profiles/shared.jpg")).thenReturn("https://images.example.invalid/shared");

        mvc.perform(get(PATH).header("Authorization", bearer(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].profileImage").value("https://images.example.invalid/shared"))
                .andExpect(jsonPath("$.result.content[1].profileImage").value("https://images.example.invalid/shared"))
                .andExpect(jsonPath("$.result.content[2].profileImage").value((Object) null));
        verify(imageUrlProvider).getImageUrl("profiles/shared.jpg");
        verifyNoMoreInteractions(imageUrlProvider);
    }

    @Test
    void rejectsStoredUrlBeforeSigningAnyProfileImages() throws Exception {
        seedExpert(10, "Valid key");
        seedExpert(11, "Invalid key");
        favorite(MEMBER, 10, TIME.plusDays(1));
        favorite(MEMBER, 11, TIME);
        jdbc.update("UPDATE member SET profile_image = 'profiles/studio.jpg' WHERE member_id = 10");
        jdbc.update("UPDATE member SET profile_image = 'https://images.example.invalid/legacy' WHERE member_id = 11");

        mvc.perform(get(PATH).header("Authorization", bearer(MEMBER)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,101", "abc,10", "0,abc", "2147483648,10"})
    void rejectsInvalidPageParameters(String page, String size) throws Exception {
        mvc.perform(get(PATH).param("page", page).param("size", size).header("Authorization", bearer(MEMBER)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
    }

    @Test
    void requiresValidJwt() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get(PATH).header("Authorization", "Bearer invalid-jwt"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void documentsPaginationAndCommonResponses() throws Exception {
        String operation = "$.paths['/api/members/me/favorites'].get";
        var response = mvc.perform(get("/api/v3/api-docs").with(httpBasic("swagger-test", "swagger-test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(operation + ".parameters.length()").value(2))
                .andExpect(jsonPath(operation + ".parameters[0].name").value("page"))
                .andExpect(jsonPath(operation + ".parameters[0].schema.default").value(0))
                .andExpect(jsonPath(operation + ".parameters[1].name").value("size"))
                .andExpect(jsonPath(operation + ".parameters[1].schema.default").value(10))
                .andExpect(jsonPath(operation + ".requestBody").doesNotExist())
                .andExpect(jsonPath(operation + ".responses['200'].content").exists())
                .andExpect(jsonPath(operation + ".responses['400'].content['application/json'].example.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath(operation + ".responses['401'].content['application/json'].example.code")
                        .value("UNAUTHORIZED"))
                .andExpect(jsonPath(operation + ".responses['500'].content['application/json'].example.code")
                        .value("INTERNAL_ERROR"))
                .andReturn().getResponse();

        JsonNode document = mapper.readTree(response.getContentAsByteArray());
        JsonNode content = document.path("paths").path(PATH).path("get").path("responses").path("200").path("content");
        JsonNode responseFields = resolveSchema(document, content.iterator().next().path("schema")).path("properties");
        JsonNode pageFields = resolveSchema(document, responseFields.path("result")).path("properties");
        JsonNode totalPages = pageFields.path("totalPages");
        assertThat(totalPages.path("type").asText()).isEqualTo("integer");
        assertThat(totalPages.path("format").asText()).isEqualTo("int64");
        JsonNode itemFields = resolveSchema(document, pageFields.path("content").path("items")).path("properties");
        for (String field : List.of("profileImage", "introduction", "startingPrice")) {
            JsonNode schema = itemFields.path(field);
            boolean allowsNull = schema.path("nullable").asBoolean(false)
                    || java.util.stream.StreamSupport.stream(schema.path("type").spliterator(), false)
                            .anyMatch(type -> "null".equals(type.asText()));
            assertThat(allowsNull).as("%s schema: %s", field, schema).isTrue();
        }
    }

    private JsonNode resolveSchema(JsonNode document, JsonNode schema) {
        String reference = schema.path("$ref").asText("");
        return reference.startsWith("#/") ? document.at(reference.substring(1)) : schema;
    }

    private void seedMember(long id, String nickname) {
        jdbc.update("""
                INSERT INTO member (member_id, nickname, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, nickname);
    }

    private void seedExpert(long id, String nickname) {
        seedMember(id, nickname);
        jdbc.update("""
                INSERT INTO expert (member_id, avg_rating, review_count, created_at, updated_at)
                VALUES (?, 4.8, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id);
    }

    private void price(long expertId, String specialty, int amount) {
        jdbc.update("""
                INSERT INTO expert_specialty (member_id, name, base_price, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, expertId, specialty, amount);
    }

    private void favorite(long memberId, long expertId, LocalDateTime time) {
        jdbc.update("INSERT INTO favorite (member_id, expert_member_id, created_at) VALUES (?, ?, ?)",
                memberId, expertId, time);
    }

    private String bearer(long memberId) {
        return "Bearer " + tokens.createAccessToken(memberId);
    }
}
