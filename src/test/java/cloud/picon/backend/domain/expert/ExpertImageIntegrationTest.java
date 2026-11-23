package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.service.ExpertImageService;
import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expert-images-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER",
        "springdoc.api-docs.enabled=true",
        "app.swagger.username=swagger-test",
        "app.swagger.password=swagger-test"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExpertImageIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwtProvider;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired ExpertImageService imageService;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean S3ImageUrlProvider imageUrlProvider;
    @MockitoSpyBean ExpertDetailRepository expertRepository;

    private String authorization;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM expert_image");
        jdbc.update("DELETE FROM expert");
        jdbc.update("DELETE FROM member");
        for (int id = 1; id <= 6; id++) {
            String nickname = switch (id) {
                case 4 -> null;
                case 5 -> " \t\n\u3000";
                default -> "expert" + id;
            };
            jdbc.update("INSERT INTO member(member_id,nickname,profile_image,created_at,updated_at) VALUES (?,?,'https://unrelated.example/profile.jpg',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", id, nickname);
            if (id != 6) {
                jdbc.update("INSERT INTO expert(member_id,created_at,updated_at) VALUES (?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", id);
            }
        }
        image(101, 1, "SHOOTING", "2026-11-10 14:30:00");
        image(102, 1, "EDITING", "2026-11-11 15:00:00");
        image(103, 1, "SHOOTING", "2026-11-11 15:00:00");
        image(201, 2, "EDITING", "2026-11-12 10:00:00");
        image(401, 4, "SHOOTING", "2026-11-13 10:00:00");
        image(501, 5, "EDITING", "2026-11-13 10:00:00");
        image(601, 6, "SHOOTING", "2026-11-13 10:00:00");
        authorization = "Bearer " + jwtProvider.createAccessToken(99L);
        when(imageUrlProvider.getImageUrl(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "https://signed.example/" + invocation.getArgument(0);
        });
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
            return invocation.callRealMethod();
        }).when(expertRepository).existsVisibleExpert(anyLong());
    }

    private void image(long id, long memberId, String specialty, String createdAt) {
        jdbc.update("INSERT INTO expert_image(expert_image_id,member_id,image_key,specialty,created_at,updated_at) VALUES (?,?,?,?,?,?)",
                id, memberId, "portfolio/" + id + ".jpg", specialty, createdAt, createdAt);
    }

    @Test
    void returnsOnlyRequestedExpertsImagesInStableLatestOrderWithUtcOffset() throws Exception {
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result.content[*].imageId", contains(103, 102, 101)))
                .andExpect(jsonPath("$.result.content[*].specialty", contains("SHOOTING", "EDITING", "SHOOTING")))
                .andExpect(jsonPath("$.result.content[0].imageUrl").value("https://signed.example/portfolio/103.jpg"))
                .andExpect(jsonPath("$.result.content[0].createdAt").value("2026-11-11T15:00:00Z"))
                .andExpect(jsonPath("$.result.content[2].createdAt").value("2026-11-10T14:30:00Z"))
                .andExpect(jsonPath("$.result.content[0].expertId").doesNotExist())
                .andExpect(jsonPath("$.result.content[0].expertNickname").doesNotExist())
                .andExpect(jsonPath("$.result.content[0].portfolioId").doesNotExist())
                .andExpect(jsonPath("$.result.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.result.content[0].tags").doesNotExist())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
        verify(expertRepository).existsVisibleExpert(1);
        verifyNoMoreInteractions(expertRepository);
        verify(imageUrlProvider, times(3)).getImageUrl(anyString());
    }

    @Test
    void nonUtcDefaultTimezoneDoesNotShiftStoredTime() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
            mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content[0].createdAt").value("2026-11-11T15:00:00Z"));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "400, INVALID_REQUEST, /api/experts/0/images",
            "401, UNAUTHORIZED, /api/experts/1/images",
            "404, EXPERT_NOT_FOUND, /api/experts/999/images",
            "500, INTERNAL_ERROR, /api/experts/1/images"
    })
    void swaggerErrorExampleMatchesHttpResponse(int statusCode, String code, String path) throws Exception {
        if (statusCode == 500) {
            when(imageUrlProvider.getImageUrl(anyString())).thenThrow(new IllegalStateException("signing failed"));
        }
        var request = get(path);
        if (statusCode != 401) {
            request.header("Authorization", authorization);
        }
        var actual = mvc.perform(request).andExpect(status().is(statusCode))
                .andReturn().getResponse().getContentAsByteArray();
        var openApi = mvc.perform(get("/api/v3/api-docs").with(httpBasic("swagger-test", "swagger-test")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String examplePath = "$.paths['/api/experts/{expertId}/images'].get.responses['" + statusCode
                + "'].content['application/json'].examples['" + code + "'].value";
        Map<?, ?> example = JsonPath.read(new String(openApi, UTF_8), examplePath);
        Map<?, ?> response = JsonPath.read(new String(actual, UTF_8), "$");
        assertThat(example).isEqualTo(response);
    }

    @Test
    void nonHttpSchemePrefixRemainsAnAllowedPortfolioKey() throws Exception {
        String key = " custom+scheme://portfolio/image.jpg";
        jdbc.update("UPDATE expert_image SET image_key = ? WHERE expert_image_id = 103", key);
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                .andExpect(status().isOk());
        verify(imageUrlProvider).getImageUrl(key);
    }

    @Test
    void anotherExpertsImagesDoNotLeak() throws Exception {
        mvc.perform(get("/api/experts/2/images").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(201)))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    void fractionalStoredTimeIsPreservedWithUtcOffset() throws Exception {
        jdbc.update("UPDATE expert_image SET created_at = '2026-11-10 14:30:00.123456' WHERE expert_image_id = 101");
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[2].createdAt").value("2026-11-10T14:30:00.123456Z"));
    }

    @Test
    void filtersImageSpecialtyAndCountsOnlyMatchingRows() throws Exception {
        mvc.perform(get("/api/experts/1/images?specialty=SHOOTING&page=1&size=1").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(101)))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(true));
        mvc.perform(get("/api/experts/1/images?specialty=EDITING&size=100").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(102)))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    void firstPageReportsMorePagesAndTieBreaksByImageId() throws Exception {
        mvc.perform(get("/api/experts/1/images?size=2").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(103, 102)))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/3/images", "/2/images?specialty=SHOOTING", "/1/images?page=100"})
    void emptyImagesFilterOrPageReturns200WithoutSigning(String path) throws Exception {
        mvc.perform(get("/api/experts" + path).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.last").value(true));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @CsvSource({"2147483647,10,1", "2147483647,100,1", "2147483647,1,3", "214748365,10,1"})
    void hugeOutOfRangePageRemainsAnEmptySuccess(int page, int size, int totalPages) throws Exception {
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization)
                        .param("page", String.valueOf(page)).param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.page").value(page))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(totalPages))
                .andExpect(jsonPath("$.result.last").value(true));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void hugeOffsetCountKeepsExpertAndSpecialtyConditions() throws Exception {
        mvc.perform(get("/api/experts/1/images?specialty=SHOOTING&page=2147483647").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(2));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(longs = {999, 6, 4, 5})
    void missingGeneralOrHiddenExpertsReturn404EvenForHugePage(long expertId) throws Exception {
        mvc.perform(get("/api/experts/" + expertId + "/images?page=2147483647").header("Authorization", authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"));
        verify(expertRepository).existsVisibleExpert(expertId);
        verifyNoMoreInteractions(expertRepository);
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\r\n", "\u00A0", "\u3000", " \t\u00A0\u3000\n"})
    void nicknameVisibilityMatchesDetailAndSearch(String nickname) throws Exception {
        jdbc.update("UPDATE member SET nickname = ? WHERE member_id = 1", nickname);
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808"})
    void invalidExpertIdsReturn400(String id) throws Exception {
        mvc.perform(get("/api/experts/" + id + "/images").header("Authorization", authorization))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(expertRepository, imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=abc", "page=2147483648", "size=0", "size=-1", "size=101",
            "size=abc", "specialty=unknown", "specialty=SHOOTING_EDITING"})
    void invalidQueryReturns400(String query) throws Exception {
        mvc.perform(get("/api/experts/1/images?" + query).header("Authorization", authorization))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(expertRepository, imageUrlProvider);
    }

    @Test
    void sameKeyWithDifferentIdsRemainsTwoRowsAndReusesSignature() throws Exception {
        jdbc.update("UPDATE expert_image SET image_key = 'portfolio/102.jpg' WHERE expert_image_id = 103");
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(103, 102, 101)))
                .andExpect(jsonPath("$.result.content[0].imageUrl").value("https://signed.example/portfolio/102.jpg"))
                .andExpect(jsonPath("$.result.content[1].imageUrl").value("https://signed.example/portfolio/102.jpg"))
                .andExpect(jsonPath("$.result.totalElements").value(3));
        verify(imageUrlProvider).getImageUrl("portfolio/102.jpg");
        verify(imageUrlProvider).getImageUrl("portfolio/101.jpg");
        verifyNoMoreInteractions(imageUrlProvider);
    }

    @Test
    void missingAndMalformedTokensReturn401() throws Exception {
        mvc.perform(get("/api/experts/1/images")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/experts/1/images").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(expertRepository, imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-1", "+99", "9223372036854775808"})
    void invalidSubjectReturns401(String subject) throws Exception {
        var claims = JwtClaimsSet.builder().issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600));
        if (subject != null) {
            claims.subject(subject);
        }
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        mvc.perform(get("/api/experts/1/images").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(expertRepository, imageUrlProvider);
    }

    @Test
    void expiredAndForgedTokensReturn401() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("99").issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(300)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/experts/1/images").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        String token = authorization.substring("Bearer ".length());
        int start = token.lastIndexOf('.') + 1;
        String forged = token.substring(0, start) + (token.charAt(start) == 'A' ? 'B' : 'A') + token.substring(start + 1);
        mvc.perform(get("/api/experts/1/images").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(expertRepository, imageUrlProvider);
    }

    @Test
    void callerTransactionIsSuspendedForSigningAndResumedAfterwards() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(imageService.getImages(1, null, 0, 10).content()).hasSize(3);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        });
        verify(imageUrlProvider, times(3)).getImageUrl(anyString());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void signingFailureUsesCommonInternalErrorWithoutLoggingStoredValues(CapturedOutput output) throws Exception {
        String key = "portfolio/103.jpg";
        String url = "https://s3.example/" + key + "?signature=private-sdk-token";
        when(imageUrlProvider.getImageUrl(anyString())).thenThrow(new IllegalStateException("Signing failed: " + url));
        mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        assertThat(output.getAll()).contains("imageId=103").doesNotContain(key).doesNotContain(url).doesNotContain("private-sdk-token");
    }

    @Nested
    @TestPropertySource(properties = {
            "spring.datasource.url=jdbc:h2:mem:expert-images-legacy-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER",
            "spring.jpa.hibernate.ddl-auto=none"
    })
    @Sql(statements = {
            """
            CREATE TABLE IF NOT EXISTS member (
                member_id BIGINT PRIMARY KEY, nickname VARCHAR(20), profile_image VARCHAR(500),
                created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL);
            """,
            """
            CREATE TABLE IF NOT EXISTS expert (
                member_id BIGINT PRIMARY KEY, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL);
            """,
            // Legacy VARCHAR data predates the specialty CHECK constraint in the Entity test schema.
            """
            CREATE TABLE IF NOT EXISTS expert_image (
                expert_image_id BIGINT PRIMARY KEY, member_id BIGINT NOT NULL, image_key VARCHAR(500) NOT NULL,
                specialty VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL);
            """
    })
    class LegacyData {
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t\r\n", "\u0085", "\u00A0", "\u3000", "https://example.com/image.jpg",
                " HTTP://example.com/image.jpg ", "//example.com/image.jpg"})
        void invalidStoredKeysFailBeforeAnySigning(String key) throws Exception {
            jdbc.update("UPDATE expert_image SET image_key = ? WHERE expert_image_id = 101", key);
            mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            verifyNoInteractions(imageUrlProvider);
        }

        @Test
        @ExtendWith(OutputCaptureExtension.class)
        void keyValidationDoesNotLogStoredValue(CapturedOutput output) throws Exception {
            String key = "https://example.com/image.jpg?secret=must-not-appear";
            jdbc.update("UPDATE expert_image SET image_key = ? WHERE expert_image_id = 101", key);
            mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                    .andExpect(status().isInternalServerError());
            assertThat(output.getAll()).contains("imageId=101").doesNotContain(key).doesNotContain("must-not-appear");
            verifyNoInteractions(imageUrlProvider);
        }

        @ParameterizedTest
        @ValueSource(strings = {"UNKNOWN", "SHOOTING_EDITING", "shooting", ""})
        void invalidStoredSpecialtyReturnsIntegrityError(String specialty) throws Exception {
            jdbc.update("UPDATE expert_image SET specialty = ? WHERE expert_image_id = 101", specialty);
            mvc.perform(get("/api/experts/1/images").header("Authorization", authorization))
                    .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            verifyNoInteractions(imageUrlProvider);
        }
    }
}
