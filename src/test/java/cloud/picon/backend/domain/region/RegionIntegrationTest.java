package cloud.picon.backend.domain.region;

import cloud.picon.backend.global.jwt.JwtProvider;
import org.junit.jupiter.api.AfterEach;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegionIntegrationTest {

    // JSON 숫자 비교가 쉽도록 int 범위의 테스트용 코드 사용 (실제 코드는 10자리)
    private static final int SEOUL = 1100;
    private static final int MAPO = 1144;
    private static final int GANGNAM = 1168;
    private static final int MAPO_DONG = 114401;
    private static final int SEJONG = 3611;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtProvider jwtProvider;

    private String authorization;

    @BeforeEach
    void setUp() {
        cleanUp();
        insertRegion(SEOUL, "서울", null, null);
        insertRegion(MAPO, "서울", "마포구", null);
        insertRegion(GANGNAM, "서울", "강남구", null);
        insertRegion(MAPO_DONG, "서울", "마포구", "합정동");
        insertRegion(SEJONG, "세종", null, null);

        insertExpert(1, "expert1");
        insertExpert(2, "expert2");
        insertExpert(3, null);   // 닉네임 없음: 전문가 검색에 안 나오므로 수에서 제외
        insertExpert(4, "   ");  // 공백 닉네임: 제외
        insertExpertRegion(101, 1, MAPO);
        insertExpertRegion(102, 2, MAPO);
        insertExpertRegion(103, 2, GANGNAM);
        insertExpertRegion(104, 3, MAPO);
        insertExpertRegion(105, 4, MAPO);
        insertExpertRegion(106, 1, SEJONG);
        insertExpertRegion(107, 2, MAPO_DONG);

        authorization = "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM expert_region");
        jdbc.update("DELETE FROM expert");
        jdbc.update("DELETE FROM member");
        jdbc.update("DELETE FROM region");
    }

    private void insertRegion(int code, String first, String second, String third) {
        jdbc.update("INSERT INTO region(region_code, first_depth, second_depth, third_depth) VALUES (?, ?, ?, ?)",
                code, first, second, third);
    }

    private void insertExpert(long id, String nickname) {
        jdbc.update("INSERT INTO member(member_id, nickname, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, nickname);
        jdbc.update("INSERT INTO expert(member_id, created_at, updated_at) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id);
    }

    private void insertExpertRegion(long id, long memberId, int regionCode) {
        jdbc.update("INSERT INTO expert_region(expert_region_id, member_id, region_code, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                id, memberId, regionCode);
    }

    @Test
    void returnsDistrictsAndProvincesWithoutDistrictsWithVisibleExpertCounts() throws Exception {
        mvc.perform(get("/api/regions").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.result[*].regionCode", contains(MAPO, GANGNAM, SEJONG)))
                .andExpect(jsonPath("$.result[0].firstDepth").value("서울"))
                .andExpect(jsonPath("$.result[0].secondDepth").value("마포구"))
                .andExpect(jsonPath("$.result[0].expertCount").value(2))
                .andExpect(jsonPath("$.result[1].expertCount").value(1))
                .andExpect(jsonPath("$.result[2].firstDepth").value("세종"))
                .andExpect(jsonPath("$.result[2].secondDepth").value(nullValue()))
                .andExpect(jsonPath("$.result[2].expertCount").value(1));
    }

    @Test
    void keywordMatchesDistrictName() throws Exception {
        mvc.perform(get("/api/regions").param("keyword", "마포").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].regionCode", contains(MAPO)))
                .andExpect(jsonPath("$.result[0].expertCount").value(2));
    }

    @Test
    void keywordMatchesProvinceNameAndIsStripped() throws Exception {
        mvc.perform(get("/api/regions").param("keyword", "  서울  ").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[*].regionCode", contains(MAPO, GANGNAM)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankKeywordReturnsAllRegions(String keyword) throws Exception {
        mvc.perform(get("/api/regions").param("keyword", keyword).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(3)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!"})
    void likeWildcardsAreSearchedAsPlainText(String keyword) throws Exception {
        mvc.perform(get("/api/regions").param("keyword", keyword).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));
    }

    @Test
    void keywordLongerThanTwentyCharactersIsRejected() throws Exception {
        mvc.perform(get("/api/regions").param("keyword", "가".repeat(21)).header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void keywordOfTwentyCharactersIsAccepted() throws Exception {
        mvc.perform(get("/api/regions").param("keyword", "가".repeat(20)).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/regions"))
                .andExpect(status().isUnauthorized());
    }
}