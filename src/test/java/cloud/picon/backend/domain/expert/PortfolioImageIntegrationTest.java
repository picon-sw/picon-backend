package cloud.picon.backend.domain.expert;

import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioImageIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtProvider jwtProvider;
    @Autowired JwtEncoder jwtEncoder;
    @MockitoBean S3ImageUrlProvider imageUrlProvider;

    private String authorization;

    @BeforeEach
    void setUp() {
        cleanUp();
        for (int id = 1; id <= 5; id++) {
            String nickname = switch (id) {
                case 3 -> null;
                case 4 -> "   ";
                default -> "expert" + id;
            };
            jdbc.update("INSERT INTO member(member_id, nickname, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    id, nickname);
            if (id != 2) {
                jdbc.update("INSERT INTO expert(member_id, created_at, updated_at) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id);
            }
        }
        insertImage(101, 1, "SHOOTING", "2026-01-01 10:00:00");
        insertImage(102, 1, "EDITING", "2026-01-02 10:00:00");
        insertImage(103, 1, "SHOOTING", "2026-01-02 10:00:00");
        insertImage(104, 2, "SHOOTING", "2026-01-03 10:00:00");
        insertImage(105, 3, "SHOOTING", "2026-01-03 10:00:00");
        insertImage(106, 4, "SHOOTING", "2026-01-03 10:00:00");
        insertImage(107, 5, "EDITING", "2026-01-01 09:00:00");
        when(imageUrlProvider.getImageUrl(anyString())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "https://signed.example/" + call.getArgument(0);
        });
        authorization = "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM expert_image");
        jdbc.update("DELETE FROM expert");
        jdbc.update("DELETE FROM member");
    }

    private void insertImage(long id, long memberId, String specialty, String createdAt) {
        jdbc.update("INSERT INTO expert_image(expert_image_id, member_id, image_key, specialty, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                id, memberId, "portfolio/" + id + ".jpg", specialty, createdAt, createdAt);
    }

    @Test
    void defaultPageUsesStableLatestOrderAndExcludesUnregisteredOrUnnamedMembers() throws Exception {
        mvc.perform(get("/api/portfolio-images").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result.content[*].imageId", contains(103, 102, 101, 107)))
                .andExpect(jsonPath("$.result.content[0].expertId").value(1))
                .andExpect(jsonPath("$.result.content[0].expertNickname").value("expert1"))
                .andExpect(jsonPath("$.result.content[0].imageUrl").value("https://signed.example/portfolio/103.jpg"))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(4))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
    }

    @Test
    void shootingFilterAndCountUseTheSameVisibilityConditions() throws Exception {
        mvc.perform(get("/api/portfolio-images?specialty=SHOOTING&size=1&page=1")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(101)))
                .andExpect(jsonPath("$.result.content[0].specialty").value("SHOOTING"))
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(true));
    }

    @Test
    void editingFilterAndMaximumSizeAreSupported() throws Exception {
        mvc.perform(get("/api/portfolio-images?specialty=EDITING&size=100")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(102, 107)))
                .andExpect(jsonPath("$.result.totalElements").value(2));
    }

    @Test
    void firstPageReportsMorePages() throws Exception {
        mvc.perform(get("/api/portfolio-images?size=2").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].imageId", contains(103, 102)))
                .andExpect(jsonPath("$.result.totalElements").value(4))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(false));
    }

    @Test
    void outOfRangePageIsEmptyAndDoesNotSignUrls() throws Exception {
        mvc.perform(get("/api/portfolio-images?page=100").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(4));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @CsvSource({"2147483647,10,1", "2147483647,100,1", "2147483647,1,4",
            "2147483646,1,4", "214748365,10,1"})
    void veryLargeOutOfRangePageReturnsEmptySuccess(int page, int size, int totalPages) throws Exception {
        mvc.perform(get("/api/portfolio-images").param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(4))
                .andExpect(jsonPath("$.result.page").value(page))
                .andExpect(jsonPath("$.result.size").value(size))
                .andExpect(jsonPath("$.result.totalPages").value(totalPages))
                .andExpect(jsonPath("$.result.last").value(true));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void veryLargePageCountRetainsSpecialtyFilter() throws Exception {
        mvc.perform(get("/api/portfolio-images?page=2147483647&specialty=SHOOTING")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(2));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void noImagesReturnsEmptySuccess() throws Exception {
        jdbc.update("DELETE FROM expert_image");
        mvc.perform(get("/api/portfolio-images").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(0))
                .andExpect(jsonPath("$.result.totalPages").value(0))
                .andExpect(jsonPath("$.result.last").value(true));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=-1", "size=101", "page=abc", "size=abc",
            "page=2147483648", "specialty=SHOOTING_EDITING", "specialty=unknown", "tag=portrait", "tag="})
    void invalidParametersReturnCommonBadRequest(String query) throws Exception {
        mvc.perform(get("/api/portfolio-images?" + query).header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void missingTokenReturnsCommonUnauthorized() throws Exception {
        mvc.perform(get("/api/portfolio-images"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void invalidTokenReturnsCommonUnauthorized() throws Exception {
        mvc.perform(get("/api/portfolio-images").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void expiredTokenReturnsCommonUnauthorized() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1")
                .issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(300)).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/portfolio-images").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void forgedSignatureReturnsCommonUnauthorized() throws Exception {
        String token = authorization.substring("Bearer ".length());
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String forged = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        mvc.perform(get("/api/portfolio-images").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void signingFailureReturnsExistingInternalError() throws Exception {
        when(imageUrlProvider.getImageUrl(anyString())).thenThrow(new IllegalStateException("signing failed"));
        mvc.perform(get("/api/portfolio-images").header("Authorization", authorization))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
