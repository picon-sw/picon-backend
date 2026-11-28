package cloud.picon.backend.domain.region.repository;

import cloud.picon.backend.domain.region.dto.RegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegionQueryRepository {

    // 전문가 검색(ExpertSearchRepository)과 같은 공개 기준: 닉네임에 공백이 아닌 문자가 있어야 함
    private static final String NON_WHITESPACE_PATTERN =
            "[^\t-\r \u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]";

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * 시군구 단위 지역과 지역별 활동 전문가 수를 조회한다.
     * 읍면동은 제외하고, 시군구가 없는 시도(세종 등)는 시도 행으로 포함한다.
     */
    public List<RegionResponse> findDistricts(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.region_code, r.first_depth, r.second_depth,
                       (SELECT COUNT(DISTINCT er.member_id)
                          FROM expert_region er
                          JOIN expert e ON e.member_id = er.member_id
                          JOIN member m ON m.member_id = e.member_id
                         WHERE er.region_code = r.region_code
                           AND m.nickname IS NOT NULL
                           AND REGEXP_LIKE(m.nickname, :nonWhitespacePattern)) AS expert_count
                FROM region r
                WHERE r.third_depth IS NULL
                  AND (r.second_depth IS NOT NULL
                       OR NOT EXISTS (SELECT 1 FROM region c
                                      WHERE c.first_depth = r.first_depth AND c.second_depth IS NOT NULL))
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource("nonWhitespacePattern", NON_WHITESPACE_PATTERN);
        if (!keyword.isEmpty()) {
            // %, _ 를 일반 문자로 검색하기 위해 escape 문자 자체부터 처리
            String escaped = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
            parameters.addValue("keyword", "%" + escaped + "%");
            sql.append("  AND (r.first_depth LIKE :keyword ESCAPE '!' OR r.second_depth LIKE :keyword ESCAPE '!')\n");
        }
        sql.append("ORDER BY r.region_code");
        return jdbc.query(sql.toString(), parameters, (rs, rowNum) -> new RegionResponse(
                rs.getLong("region_code"), rs.getString("first_depth"), rs.getString("second_depth"),
                rs.getInt("expert_count")));
    }
}