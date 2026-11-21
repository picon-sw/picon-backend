package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.dto.ExpertSpecialtyResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.expert.repository.ExpertSearchRepository;
import cloud.picon.backend.domain.expert.service.ExpertDetailService;
import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:expert-detail-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExpertDetailIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwtProvider;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired ExpertDetailService detailService;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean S3ImageUrlProvider imageUrlProvider;
    @MockitoSpyBean ExpertDetailRepository detailRepository;
    @MockitoSpyBean ExpertSearchRepository searchRepository;

    private String authorization;
    private static final String SUMMARY = "  오래된 요약\n저장된 원문  ";

    @BeforeEach
    void setUp() {
        for (String table : new String[]{"favorite", "expert_region", "expert_photo_style", "expert_specialty", "expert", "region", "photo_style", "member"}) {
            jdbc.update("DELETE FROM " + table);
        }
        member(1, "서하", "profiles/서하 a+b.jpg");
        member(2, "지훈", null);
        member(3, null, null);
        member(4, "   ", null);
        member(5, "일반회원", null);
        member(98, "다른회원", null);
        member(99, "현재회원", null);
        for (long id = 1; id <= 4; id++) {
            jdbc.update("""
                    INSERT INTO expert(member_id,career_years,avg_rating,review_count,review_summary,
                    review_summary_count,review_summary_updated_at,created_at,updated_at)
                    VALUES (?,?,?,?,?,1,'2020-01-01 00:00:00',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, id, id == 1 ? 3 : 0, id == 1 ? "4.8" : "0.0", id == 1 ? 23 : 0, id == 1 ? SUMMARY : null);
        }
        jdbc.update("INSERT INTO expert_specialty(member_id,name,base_price,created_at,updated_at) VALUES (1,'SHOOTING',80000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(1,'EDITING',30000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO region(region_code,first_depth,second_depth,third_depth) VALUES (1111000000,'서울특별시','종로구',NULL),(1165000000,'서울특별시','서초구','서초동')");
        jdbc.update("INSERT INTO expert_region(member_id,region_code,created_at) VALUES (1,1165000000,CURRENT_TIMESTAMP),(1,1111000000,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO photo_style(photo_style_id,name) VALUES (20,'프로필사진'),(10,'졸업사진')");
        jdbc.update("INSERT INTO expert_photo_style(member_id,photo_style_id,created_at) VALUES (1,20,CURRENT_TIMESTAMP),(1,10,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO favorite(member_id,expert_member_id,created_at) VALUES (99,1,CURRENT_TIMESTAMP),(98,2,CURRENT_TIMESTAMP)");
        authorization = "Bearer " + jwtProvider.createAccessToken(99L);
        when(imageUrlProvider.getImageUrl(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "https://signed.example/" + invocation.getArgument(0);
        });
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
            return invocation.callRealMethod();
        }).when(detailRepository).findExpert(anyLong(), anyLong());
    }

    private void member(long id, String nickname, String profileImage) {
        jdbc.update("""
                INSERT INTO member(member_id,nickname,profile_image,introduction,created_at,updated_at)
                VALUES (?,?,?,'자연스러운 순간',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, id, nickname, profileImage);
    }

    @Test
    void returnsDetailWithSavedSummaryFullRelationsAndExactlyFourQueries() throws Exception {
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result.expertId").value(1))
                .andExpect(jsonPath("$.result.nickname").value("서하"))
                .andExpect(jsonPath("$.result.profileImage").value("https://signed.example/profiles/서하 a+b.jpg"))
                .andExpect(jsonPath("$.result.introduction").value("자연스러운 순간"))
                .andExpect(jsonPath("$.result.careerYears").value(3))
                .andExpect(jsonPath("$.result.specialties[*].name", contains("EDITING", "SHOOTING")))
                .andExpect(jsonPath("$.result.specialties[*].basePrice", contains(30000, 80000)))
                .andExpect(jsonPath("$.result.regions[*].regionCode", contains(1111000000, 1165000000)))
                .andExpect(jsonPath("$.result.regions[*].name", contains("서울특별시 종로구", "서울특별시 서초구 서초동")))
                .andExpect(jsonPath("$.result.photoStyles[*].photoStyleId", contains(10, 20)))
                .andExpect(jsonPath("$.result.photoStyles[*].name", contains("졸업사진", "프로필사진")))
                .andExpect(jsonPath("$.result.rating").value(4.8))
                .andExpect(jsonPath("$.result.reviewCount").value(23))
                .andExpect(jsonPath("$.result.reviewSummary").value(SUMMARY))
                .andExpect(jsonPath("$.result.isFavorite").value(true))
                .andExpect(jsonPath("$.result.reviewSummaryCount").doesNotExist())
                .andExpect(jsonPath("$.result.reviewSummaryUpdatedAt").doesNotExist())
                .andExpect(jsonPath("$.result.images").doesNotExist())
                .andExpect(jsonPath("$.result.representativeImage").doesNotExist())
                .andExpect(jsonPath("$.result.reviews").doesNotExist());
        verify(detailRepository).findExpert(1, 99);
        verify(detailRepository).findPhotoStyles(1);
        verify(searchRepository).findSpecialties(List.of(1L));
        verify(searchRepository).findRegions(List.of(1L));
        verifyNoMoreInteractions(detailRepository, searchRepository);
        verify(imageUrlProvider).getImageUrl("profiles/서하 a+b.jpg");
        verifyNoMoreInteractions(imageUrlProvider);
    }

    @Test
    void emptyRelationsNullImagesAndZeroStatisticsAreReturnedWithoutOnboardingRequirement() throws Exception {
        mvc.perform(get("/api/experts/2").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profileImage").value(nullValue()))
                .andExpect(jsonPath("$.result.specialties").isEmpty())
                .andExpect(jsonPath("$.result.regions").isEmpty())
                .andExpect(jsonPath("$.result.photoStyles").isEmpty())
                .andExpect(jsonPath("$.result.rating").value(0.0))
                .andExpect(jsonPath("$.result.reviewCount").value(0))
                .andExpect(jsonPath("$.result.reviewSummary").value(nullValue()))
                .andExpect(jsonPath("$.result.isFavorite").value(false));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808", "-9223372036854775809"})
    void invalidExpertIdReturns400WithoutQueries(String id) throws Exception {
        mvc.perform(get("/api/experts/" + id).header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(detailRepository, searchRepository, imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(longs = {999, 5, 99, 3, 4})
    void missingGeneralMemberOrHiddenExpertReturnsDomain404(long id) throws Exception {
        mvc.perform(get("/api/experts/" + id).header("Authorization", authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("전문가 정보를 찾을 수 없습니다."));
        verify(detailRepository).findExpert(id, 99);
        verifyNoMoreInteractions(detailRepository);
        verifyNoInteractions(searchRepository, imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\r\n", "\u00A0", "\u3000", " \t\u00A0\u3000\n"})
    void blankNicknameUsesSameVisibilityRuleAsSearch(String nickname) throws Exception {
        jdbc.update("UPDATE member SET nickname = ? WHERE member_id = 1", nickname);
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EXPERT_NOT_FOUND"));
        verifyNoInteractions(searchRepository, imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n", "\u0085", "\u00A0", "\u3000", " \u2007\u202F\n"})
    void nullOrWhitespaceSummaryReturnsExplicitNull(String summary) throws Exception {
        jdbc.update("UPDATE expert SET review_summary = ? WHERE member_id = 1", summary);
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.reviewSummary").value(nullValue()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\r\n", "\u3000"})
    void absentProfileDoesNotAttemptSigning(String key) throws Exception {
        jdbc.update("UPDATE member SET profile_image = ? WHERE member_id = 1", key);
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.profileImage").value(nullValue()));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/p.jpg", " HTTP://example.com/p.jpg ", "//example.com/p.jpg", "s3://bucket/p.jpg"})
    void completeProfileUrlIsRejectedWithoutSigning(String image) throws Exception {
        jdbc.update("UPDATE member SET profile_image = ? WHERE member_id = 1", image);
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void profileValidationDoesNotLogStoredUrl(CapturedOutput output) throws Exception {
        String image = "https://example.com/p.jpg?private-token=must-not-appear";
        jdbc.update("UPDATE member SET profile_image = ? WHERE member_id = 1", image);
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isInternalServerError());
        assertThat(output.getAll()).contains("expertId=1").doesNotContain(image).doesNotContain("must-not-appear");
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(ints = {80000, 90000})
    void duplicateSpecialtyIsAnIntegrityErrorRegardlessOfPrice(int price) throws Exception {
        doReturn(Map.of(1L, List.of(
                new ExpertSpecialtyResponse(Specialty.SHOOTING, 80000),
                new ExpertSpecialtyResponse(Specialty.SHOOTING, price))))
                .when(searchRepository).findSpecialties(List.of(1L));
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        verify(searchRepository).findSpecialties(List.of(1L));
        verifyNoMoreInteractions(searchRepository);
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void favoriteBelongsToAuthenticatedViewer() throws Exception {
        String other = "Bearer " + jwtProvider.createAccessToken(98L);
        mvc.perform(get("/api/experts/1").header("Authorization", other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.isFavorite").value(false));
        mvc.perform(get("/api/experts/2").header("Authorization", other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.isFavorite").value(true));
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.isFavorite").value(true));
    }

    @Test
    void missingOrMalformedTokenReturns401() throws Exception {
        mvc.perform(get("/api/experts/1")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/experts/1").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(detailRepository, searchRepository, imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-1", "+99", "99.0", "9223372036854775808"})
    void invalidJwtSubjectReturns401WithoutDataAccess(String subject) throws Exception {
        var claims = JwtClaimsSet.builder().issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600));
        if (subject != null) {
            claims.subject(subject);
        }
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        mvc.perform(get("/api/experts/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(detailRepository, searchRepository, imageUrlProvider);
    }

    @Test
    void expiredAndForgedTokensReturn401() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("99").issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(300)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/experts/1").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        String token = authorization.substring("Bearer ".length());
        int start = token.lastIndexOf('.') + 1;
        String forged = token.substring(0, start) + (token.charAt(start) == 'A' ? 'B' : 'A') + token.substring(start + 1);
        mvc.perform(get("/api/experts/1").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(detailRepository, searchRepository, imageUrlProvider);
    }

    @Test
    void callerTransactionIsSuspendedDuringSigningAndResumedAfterwards() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(detailService.getDetail(1, 99).expertId()).isEqualTo(1);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        });
        verify(imageUrlProvider).getImageUrl("profiles/서하 a+b.jpg");
    }

    @Test
    void signingFailureUsesExistingInternalError() throws Exception {
        when(imageUrlProvider.getImageUrl(anyString())).thenThrow(new IllegalStateException("signing failed"));
        mvc.perform(get("/api/experts/1").header("Authorization", authorization))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
