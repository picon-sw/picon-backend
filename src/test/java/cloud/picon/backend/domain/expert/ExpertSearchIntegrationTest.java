package cloud.picon.backend.domain.expert;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.service.ExpertQueryService;
import cloud.picon.backend.domain.expert.service.ExpertService;
import cloud.picon.backend.domain.expert.repository.ExpertSearchRepository;
import cloud.picon.backend.global.jwt.JwtProvider;
import cloud.picon.backend.global.s3.S3ImageUrlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:expert-search-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MEMBER")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ExpertSearchIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired ExpertQueryService queryService;
    @Autowired ExpertService expertService;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwtProvider;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean S3ImageUrlProvider imageUrlProvider;
    @MockitoSpyBean ExpertSearchRepository repository;

    private String authorization;

    @BeforeEach
    void setUp() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS expert_specialty (expert_specialty_id BIGINT PRIMARY KEY, member_id BIGINT, name VARCHAR(20), base_price INT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS photo_style (photo_style_id BIGINT PRIMARY KEY, name VARCHAR(20))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS expert_photo_style (expert_photo_style_id BIGINT PRIMARY KEY, member_id BIGINT, photo_style_id BIGINT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS region (region_code BIGINT PRIMARY KEY, first_depth VARCHAR(20), second_depth VARCHAR(20), third_depth VARCHAR(20))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS expert_region (expert_region_id BIGINT PRIMARY KEY, member_id BIGINT, region_code BIGINT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS favorite (favorite_id BIGINT PRIMARY KEY, member_id BIGINT, expert_member_id BIGINT)");
        for (String table : new String[]{"favorite", "expert_image", "expert_region", "expert_photo_style", "expert_specialty", "expert", "photo_style", "region", "member"}) {
            jdbc.update("DELETE FROM " + table);
        }
        member(1, "서하", "자연스러운 순간");
        member(2, "지훈", "졸업 전문");
        member(3, "은슬", "편집");
        member(4, "!100%_shot", null);
        member(5, null, "hidden");
        member(6, "   ", "hidden");
        member(7, "not-expert", "hidden");
        member(99, "viewer", null);
        for (int id = 1; id <= 6; id++) {
            jdbc.update("INSERT INTO expert(member_id, career_years, avg_rating, review_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)", id, id == 1 ? 3 : 5,
                    id <= 3 ? "4.9" : "3.5", id == 1 ? 20 : 30,
                    id == 1 ? "2026-01-01 00:00:00" : "2026-01-02 00:00:00", "2026-01-02 00:00:00");
        }
        jdbc.update("INSERT INTO expert_specialty VALUES (1,1,'SHOOTING',80000),(2,1,'EDITING',30000),(3,2,'SHOOTING',50000),(4,2,'EDITING',90000),(5,3,'SHOOTING',100000),(6,3,'EDITING',20000),(7,4,'SHOOTING',0)");
        jdbc.update("INSERT INTO photo_style VALUES (10,'졸업사진'),(11,'차분한')");
        jdbc.update("INSERT INTO expert_photo_style VALUES (1,1,10),(2,2,10),(3,3,11),(4,1,10)");
        jdbc.update("INSERT INTO region VALUES (1111000000,'서울특별시','종로구',NULL),(1165000000,'서울특별시','서초구',NULL)");
        jdbc.update("INSERT INTO expert_region VALUES (1,1,1111000000),(2,1,1165000000),(3,2,1111000000),(4,3,1165000000),(5,1,1111000000)");
        jdbc.update("INSERT INTO favorite VALUES (1,99,1),(2,99,1),(3,98,2)");
        image(101, 1, "SHOOTING", "2026-01-01 00:00:00");
        image(102, 1, "SHOOTING", "2026-01-01 00:00:00");
        image(103, 1, "EDITING", "2026-01-03 00:00:00");
        image(201, 2, "EDITING", "2026-01-01 00:00:00");
        authorization = "Bearer " + jwtProvider.createAccessToken(99L);
        when(imageUrlProvider.getImageUrl(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "https://signed.example/" + invocation.getArgument(0);
        });
    }

    private void member(long id, String name, String introduction) {
        jdbc.update("INSERT INTO member(member_id,nickname,introduction,created_at,updated_at) VALUES (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", id, name, introduction);
    }

    private void image(long id, long memberId, String specialty, String createdAt) {
        jdbc.update("INSERT INTO expert_image(expert_image_id,member_id,image_key,specialty,created_at,updated_at) VALUES (?,?,?,?,?,?)",
                id, memberId, "portfolio/" + id + ".jpg", specialty, createdAt, createdAt);
    }

    private ExpertSearchCondition condition(String keyword, Specialty specialty, Long style, Long region,
                                            Integer min, Integer max, Integer career, ExpertSort sort, int page, int size) {
        return new ExpertSearchCondition(keyword, specialty, style, region, min, max, career, sort, page, size);
    }

    private ExpertSearchData search(String keyword) {
        return queryService.search(condition(keyword, null, null, null, null, null, null, ExpertSort.RATING, 0, 10), 99);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", "\r", "\r\n", "\f", "\u000B", " \t\r\n\f ",
            "\u0085", "\u00A0", "\u1680", "\u2000", "\u2007", "\u200A", "\u2028", "\u2029", "\u202F", "\u205F", "\u3000", " \t\u00A0\u3000\n"})
    void blankNicknamesAreExcludedBeforePaginationAndCount(String nickname) throws Exception {
        // 원래 첫 번째 전문가를 숨겨 DB의 LIMIT 적용 전에 제외되는지 확인한다.
        jdbc.update("UPDATE member SET nickname = ? WHERE member_id = 3", nickname);
        mvc.perform(get("/api/experts?size=2").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].expertId", contains(2, 1)))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(false));
        mvc.perform(get("/api/experts?size=2&page=1").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].expertId", contains(4)))
                .andExpect(jsonPath("$.result.totalElements").value(3))
                .andExpect(jsonPath("$.result.last").value(true));
        // 소개 검색으로 매칭되더라도 닉네임 노출 조건은 동일하게 적용한다.
        mvc.perform(get("/api/experts").param("keyword", "편집").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\t은슬\n", " \r은 슬\t", "\u3000은슬\u00A0", "\\t"})
    void nicknamesWithNonWhitespaceCharactersRemainVisibleAndUnchanged(String nickname) throws Exception {
        jdbc.update("UPDATE member SET nickname = ? WHERE member_id = 3", nickname);
        mvc.perform(get("/api/experts?size=2").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].expertId", contains(3, 2)))
                .andExpect(jsonPath("$.result.content[0].nickname").value(nickname))
                .andExpect(jsonPath("$.result.totalElements").value(4));
    }

    @Test
    void defaultOrderIsExpertBasedAndRelationshipsDoNotMultiplyCards() {
        var result = search(null);
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.content()).extracting(ExpertData::expertId).containsExactly(3L, 2L, 1L, 4L);
        var expert = result.content().get(2);
        assertThat(expert.specialties()).hasSize(2);
        assertThat(expert.regions()).extracting(ExpertRegionResponse::regionCode).containsExactly(1111000000L, 1165000000L);
        assertThat(expert.regions().getFirst().name()).isEqualTo("서울특별시 종로구");
        assertThat(expert.isFavorite()).isTrue();
        assertThat(result.content().get(1).isFavorite()).isFalse();
    }

    @Test
    void keywordSearchesNicknameIntroductionAndExpertStyle() {
        assertThat(search(" 서하 ").content()).extracting(ExpertData::expertId).containsExactly(1L);
        assertThat(search("자연스러운").content()).extracting(ExpertData::expertId).containsExactly(1L);
        assertThat(search("졸업사진").content()).extracting(ExpertData::expertId).containsExactly(2L, 1L);
        assertThat(search("졸업 전문").content()).extracting(ExpertData::expertId).containsExactly(2L);
        assertThat(search("   ").totalElements()).isEqualTo(4);
    }

    @Test
    void percentUnderscoreAndEscapeCharacterAreLiteral() {
        for (String keyword : new String[]{"%", "_", "!", "!100%_"}) {
            assertThat(search(keyword).content()).extracting(ExpertData::expertId).containsExactly(4L);
        }
        assertThat(search("' OR 1=1 --").content()).isEmpty();
    }

    @Test
    void priceAndSpecialtyMustMatchTheSameRow() {
        var shooting = queryService.search(condition(null, Specialty.SHOOTING, null, null, null, 40000, null, ExpertSort.RATING, 0, 10), 99);
        assertThat(shooting.content()).extracting(ExpertData::expertId).containsExactly(4L);
        var range = queryService.search(condition(null, null, null, null, 40000, 70000, null, ExpertSort.RATING, 0, 10), 99);
        assertThat(range.content()).extracting(ExpertData::expertId).containsExactly(2L);
        assertThat(range.content().getFirst().specialties()).hasSize(2);
        var editing = queryService.search(condition(null, Specialty.EDITING, null, null, 30000, 30000, null, ExpertSort.RATING, 0, 10), 99);
        assertThat(editing.content()).extracting(ExpertData::expertId).containsExactly(1L);
        var free = queryService.search(condition(null, null, null, null, 0, 0, null, ExpertSort.RATING, 0, 10), 99);
        assertThat(free.content()).extracting(ExpertData::expertId).containsExactly(4L);
    }

    @Test
    void allFiltersAreAndedAndUnknownIdsReturnEmpty() {
        var result = queryService.search(condition("졸업", Specialty.SHOOTING, 10L, 1111000000L, 40000, 90000, 4, ExpertSort.RATING, 0, 10), 99);
        assertThat(result.content()).extracting(ExpertData::expertId).containsExactly(2L);
        assertThat(result.totalElements()).isEqualTo(1);
        // 실제 FK가 없는 ERD에서도 존재하지 않는 마스터 ID는 노출하지 않는다.
        jdbc.update("INSERT INTO expert_photo_style VALUES (99,1,999)");
        jdbc.update("INSERT INTO expert_region VALUES (99,1,999)");
        assertThat(queryService.search(condition(null, null, 999L, null, null, null, null, ExpertSort.RATING, 0, 10), 99).content()).isEmpty();
        assertThat(queryService.search(condition(null, null, null, 999L, null, null, null, ExpertSort.RATING, 0, 10), 99).content()).isEmpty();
    }

    @Test
    void representativeImageUsesSpecialtyThenLatestAndFallsBack() {
        var all = search(null);
        assertThat(all.content().get(2).representativeImageKey()).isEqualTo("portfolio/103.jpg");
        assertThat(all.content().getFirst().representativeImageKey()).isNull();
        var shooting = queryService.search(condition(null, Specialty.SHOOTING, null, null, null, null, null, ExpertSort.RATING, 0, 10), 99);
        assertThat(shooting.content().get(2).representativeImageKey()).isEqualTo("portfolio/102.jpg");
        assertThat(shooting.content().get(1).representativeImageKey()).isEqualTo("portfolio/201.jpg");
        assertThat(shooting.content().getFirst().representativeImageKey()).isNull();
    }

    @Test
    void paginationCountsExpertsAndKeepsStableOrder() {
        var first = queryService.search(condition(null, null, null, null, null, null, null, ExpertSort.RATING, 0, 2), 99);
        var second = queryService.search(condition(null, null, null, null, null, null, null, ExpertSort.RATING, 1, 2), 99);
        assertThat(first.content()).extracting(ExpertData::expertId).containsExactly(3L, 2L);
        assertThat(second.content()).extracting(ExpertData::expertId).containsExactly(1L, 4L);
        assertThat(second.totalElements()).isEqualTo(4);
        var latest = queryService.search(condition(null, null, null, null, null, null, null, ExpertSort.LATEST, 0, 10), 99);
        assertThat(latest.content()).extracting(ExpertData::expertId).containsExactly(4L, 3L, 2L, 1L);
        var distant = queryService.search(condition(null, null, null, null, null, null, null, ExpertSort.RATING, Integer.MAX_VALUE, 100), 99);
        assertThat(distant.content()).isEmpty();
        assertThat(distant.totalElements()).isEqualTo(4);
        assertThat(ExpertPageResponse.from(distant, java.util.List.of()).last()).isTrue();
    }

    @Test
    void httpResponseUsesCommonWrapperFullRelationsAndViewerFavorite() throws Exception {
        mvc.perform(get("/api/experts").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result.content[*].expertId", contains(3, 2, 1, 4)))
                .andExpect(jsonPath("$.result.content[2].isFavorite").value(true))
                .andExpect(jsonPath("$.result.content[1].isFavorite").value(false))
                .andExpect(jsonPath("$.result.content[2].specialties.length()").value(2))
                .andExpect(jsonPath("$.result.content[2].regions.length()").value(2))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(4))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.last").value(true));
        verify(repository).count(any());
        verify(repository).findPage(any(), eq(99L));
        verify(repository).findSpecialties(anyList());
        verify(repository).findRegions(anyList());
        verify(repository).findRepresentativeImages(anyList(), isNull());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void httpFiltersAndAllSpecialtiesAreReturned() throws Exception {
        mvc.perform(get("/api/experts").header("Authorization", authorization)
                        .param("keyword", " 졸업 ").param("specialty", "SHOOTING")
                        .param("minPrice", "40000").param("maxPrice", "90000")
                        .param("minCareerYears", "4").param("photoStyleId", "10").param("regionCode", "1111000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].expertId", contains(2)))
                .andExpect(jsonPath("$.result.content[0].specialties.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].representativeImage").value("https://signed.example/portfolio/201.jpg"));
    }

    @Test
    void httpRepresentativeImageUsesFieldPriorityAndTieBreak() throws Exception {
        mvc.perform(get("/api/experts").header("Authorization", authorization)
                        .param("keyword", "서하").param("specialty", "SHOOTING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].representativeImage").value("https://signed.example/portfolio/102.jpg"));
        verify(imageUrlProvider).getImageUrl("portfolio/102.jpg");
    }

    @Test
    void httpLatestAndPaginationUseStableExpertOrder() throws Exception {
        mvc.perform(get("/api/experts").header("Authorization", authorization)
                        .param("sort", "latest").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].expertId", contains(2, 1)))
                .andExpect(jsonPath("$.result.totalElements").value(4))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.last").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"keyword=missing", "regionCode=999", "photoStyleId=999", "page=100", "page=2147483647&size=100"})
    void httpEmptyResultsAndOutOfRangePagesDoNotSignImages(String query) throws Exception {
        mvc.perform(get("/api/experts?" + query).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isEmpty())
                .andExpect(jsonPath("$.result.last").value(true));
        verifyNoInteractions(imageUrlProvider);
        verify(repository).count(any());
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sort=unknown", "sort=RATING", "sort=", "sort=rating%3BDROP",
            "specialty=SHOOTING_EDITING", "specialty=unknown", "page=-1", "page=abc", "page=2147483648",
            "size=0", "size=-1", "size=101", "size=abc", "minPrice=-1", "maxPrice=-1",
            "minPrice=10&maxPrice=9", "minPrice=abc", "maxPrice=2147483648",
            "minCareerYears=-1", "minCareerYears=abc", "photoStyleId=abc", "regionCode=abc"})
    void httpInvalidParametersReturn400WithoutQueries(String query) throws Exception {
        mvc.perform(get("/api/experts?" + query).header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(repository, imageUrlProvider);
    }

    @Test
    void zeroPriceAndMaximumPageSizeAreValid() throws Exception {
        mvc.perform(get("/api/experts?minPrice=0&maxPrice=0&size=100").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[*].expertId", contains(4)))
                .andExpect(jsonPath("$.result.content[0].profileImage").value(nullValue()))
                .andExpect(jsonPath("$.result.content[0].representativeImage").value(nullValue()));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void profileKeyIsSignedOnceAndOriginalKeyIsPreserved() throws Exception {
        jdbc.update("UPDATE member SET profile_image = ? WHERE member_id = 1", "portfolio/103.jpg");
        mvc.perform(get("/api/experts?keyword=서하").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].profileImage").value("https://signed.example/portfolio/103.jpg"))
                .andExpect(jsonPath("$.result.content[0].representativeImage").value("https://signed.example/portfolio/103.jpg"));
        verify(imageUrlProvider, times(1)).getImageUrl("portfolio/103.jpg");
        verifyNoMoreInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void absentProfileReturnsExplicitNullWithoutSigning(String profile) throws Exception {
        jdbc.update("UPDATE member SET profile_image = ? WHERE member_id = 4", profile);
        mvc.perform(get("/api/experts?keyword=!100").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].profileImage").value(nullValue()))
                .andExpect(jsonPath("$.result.content[0].representativeImage").value(nullValue()));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/p.jpg", "http://example.com/p.jpg", " HTTPS://example.com/p.jpg ",
            "//example.com/p.jpg", "s3://bucket/p.jpg"})
    void completeProfileUrlsAreRejectedBeforeAnySigning(String profile) throws Exception {
        jdbc.update("UPDATE member SET profile_image = ? WHERE member_id = 1", profile);
        mvc.perform(get("/api/experts?keyword=서하").header("Authorization", authorization))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        verifyNoInteractions(imageUrlProvider);
    }

    @ParameterizedTest
    @ValueSource(ints = {80000, 90000})
    void duplicateSpecialtyFailsEvenWithSamePriceOrFilterOnOtherField(int price) throws Exception {
        jdbc.update("INSERT INTO expert_specialty VALUES (99,1,'SHOOTING',?)", price);
        mvc.perform(get("/api/experts?keyword=서하&specialty=EDITING").header("Authorization", authorization))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        verifyNoInteractions(imageUrlProvider);
    }

    @Test
    void tokenMissingOrInvalidReturns401() throws Exception {
        mvc.perform(get("/api/experts")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/experts").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(repository, imageUrlProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-1", "+99", "99.0", "9223372036854775808"})
    void signedTokenWithInvalidMemberIdReturns401(String subject) throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuedAt(now).expiresAt(now.plusSeconds(600));
        if (subject != null) {
            claims.subject(subject);
        }
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        mvc.perform(get("/api/experts").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(repository, imageUrlProvider);
    }

    @Test
    void expiredOrForgedTokenReturns401() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("99").issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(300)).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/experts").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        String token = authorization.substring("Bearer ".length());
        int start = token.lastIndexOf('.') + 1;
        String forged = token.substring(0, start) + (token.charAt(start) == 'A' ? 'B' : 'A') + token.substring(start + 1);
        mvc.perform(get("/api/experts").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(repository, imageUrlProvider);
    }

    @Test
    void favoriteUsesAuthenticatedViewerInsteadOfExpertId() throws Exception {
        mvc.perform(get("/api/experts").header("Authorization", "Bearer " + jwtProvider.createAccessToken(98L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[1].isFavorite").value(true))
                .andExpect(jsonPath("$.result.content[2].isFavorite").value(false));
    }

    @Test
    void callerTransactionIsSuspendedWhileSigningAndResumedAfterSearch() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            var result = expertService.search(condition("서하", null, null, null, null, null, null, ExpertSort.RATING, 0, 10), 99);
            assertThat(result.content()).hasSize(1);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        });
        verify(imageUrlProvider).getImageUrl("portfolio/103.jpg");
    }

    @Test
    void signingFailureReturnsCommonInternalError() throws Exception {
        when(imageUrlProvider.getImageUrl(anyString())).thenThrow(new IllegalStateException("signing failed"));
        mvc.perform(get("/api/experts?keyword=서하").header("Authorization", authorization))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
