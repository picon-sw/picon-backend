package cloud.picon.backend.domain.expert.repository;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.domain.expert.entity.Specialty;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ExpertSearchRepository {

    // Unicode White_Space를 명시해 MySQL ICU와 H2 Java 정규식의 공백 판정을 일치시킨다.
    // ASCII U+0009~U+000D는 탭·개행·수직 탭·폼 피드·캐리지 리턴이다.
    private static final String NON_WHITESPACE_PATTERN =
            "[^\t-\r \u0085\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]";

    private final NamedParameterJdbcTemplate jdbc;

    public record ExpertRow(Long id, String nickname, String profileImage, String introduction,
                            int careerYears, BigDecimal rating, int reviewCount, boolean favorite) {
    }

    private record Filter(String sql, MapSqlParameterSource parameters) {
    }

    private Filter filter(ExpertSearchCondition condition) {
        StringBuilder sql = new StringBuilder("""
                FROM expert e JOIN member m ON m.member_id = e.member_id
                WHERE m.nickname IS NOT NULL AND REGEXP_LIKE(m.nickname, :nonWhitespacePattern)
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource("nonWhitespacePattern", NON_WHITESPACE_PATTERN);
        if (!condition.keyword().isEmpty()) {
            // escape 문자 자체도 먼저 처리해 %, _를 일반 문자로 검색한다.
            String escaped = condition.keyword().replace("!", "!!").replace("%", "!%").replace("_", "!_");
            parameters.addValue("keyword", "%" + escaped + "%");
            sql.append("""
                    AND (m.nickname LIKE :keyword ESCAPE '!' OR m.introduction LIKE :keyword ESCAPE '!'
                         OR EXISTS (SELECT 1 FROM expert_photo_style eps
                                    JOIN photo_style ps ON ps.photo_style_id = eps.photo_style_id
                                    WHERE eps.member_id = e.member_id AND ps.name LIKE :keyword ESCAPE '!'))
                    """);
        }
        if (condition.specialty() != null || condition.minPrice() != null || condition.maxPrice() != null) {
            sql.append("AND EXISTS (SELECT 1 FROM expert_specialty es WHERE es.member_id = e.member_id ");
            if (condition.specialty() != null) {
                sql.append("AND es.name = :specialty ");
                parameters.addValue("specialty", condition.specialty().name());
            } else {
                sql.append("AND es.name IN ('SHOOTING', 'EDITING') ");
            }
            if (condition.minPrice() != null) {
                sql.append("AND es.base_price >= :minPrice ");
                parameters.addValue("minPrice", condition.minPrice());
            }
            if (condition.maxPrice() != null) {
                sql.append("AND es.base_price <= :maxPrice ");
                parameters.addValue("maxPrice", condition.maxPrice());
            }
            sql.append(")\n");
        }
        if (condition.photoStyleId() != null) {
            sql.append("AND EXISTS (SELECT 1 FROM expert_photo_style eps JOIN photo_style ps ON ps.photo_style_id = eps.photo_style_id WHERE eps.member_id = e.member_id AND eps.photo_style_id = :photoStyleId)\n");
            parameters.addValue("photoStyleId", condition.photoStyleId());
        }
        if (condition.regionCode() != null) {
            sql.append("AND EXISTS (SELECT 1 FROM expert_region er JOIN region r ON r.region_code = er.region_code WHERE er.member_id = e.member_id AND er.region_code = :regionCode)\n");
            parameters.addValue("regionCode", condition.regionCode());
        }
        if (condition.minCareerYears() != null) {
            sql.append("AND e.career_years >= :minCareerYears\n");
            parameters.addValue("minCareerYears", condition.minCareerYears());
        }
        return new Filter(sql.toString(), parameters);
    }

    public long count(ExpertSearchCondition condition) {
        Filter filter = filter(condition);
        return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) " + filter.sql(), filter.parameters(), Long.class));
    }

    public List<ExpertRow> findPage(ExpertSearchCondition condition, long memberId) {
        Filter filter = filter(condition);
        String order = switch (condition.sort()) {
            case RATING -> "e.avg_rating DESC, e.review_count DESC, e.member_id DESC";
            case LATEST -> "e.created_at DESC, e.member_id DESC";
        };
        String sql = """
                SELECT e.member_id, m.nickname, m.profile_image, m.introduction,
                       e.career_years, e.avg_rating, e.review_count,
                       EXISTS (SELECT 1 FROM favorite f WHERE f.member_id = :viewerId
                               AND f.expert_member_id = e.member_id) AS favorite
                """ + filter.sql() + " ORDER BY " + order + " LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, filter.parameters().addValue("viewerId", memberId)
                        .addValue("limit", condition.size()).addValue("offset", condition.offset()),
                (rs, index) -> new ExpertRow(rs.getLong("member_id"), rs.getString("nickname"),
                        rs.getString("profile_image"), rs.getString("introduction"), rs.getInt("career_years"),
                        rs.getBigDecimal("avg_rating"), rs.getInt("review_count"), rs.getBoolean("favorite")));
    }

    public Map<Long, List<ExpertSpecialtyResponse>> findSpecialties(List<Long> ids) {
        Map<Long, List<ExpertSpecialtyResponse>> result = new HashMap<>();
        jdbc.query("""
                SELECT member_id, name, base_price FROM expert_specialty
                WHERE member_id IN (:ids) ORDER BY member_id, name, expert_specialty_id
                """, new MapSqlParameterSource("ids", ids), rs -> {
            result.computeIfAbsent(rs.getLong("member_id"), ignored -> new ArrayList<>())
                    .add(new ExpertSpecialtyResponse(Specialty.valueOf(rs.getString("name")), rs.getInt("base_price")));
        });
        return result;
    }

    public Map<Long, List<ExpertRegionResponse>> findRegions(List<Long> ids) {
        Map<Long, List<ExpertRegionResponse>> result = new HashMap<>();
        jdbc.query("""
                SELECT DISTINCT er.member_id, r.region_code, r.first_depth, r.second_depth, r.third_depth
                FROM expert_region er JOIN region r ON r.region_code = er.region_code
                WHERE er.member_id IN (:ids) ORDER BY er.member_id, r.region_code
                """, new MapSqlParameterSource("ids", ids), rs -> {
            String name = java.util.stream.Stream.of(rs.getString("first_depth"), rs.getString("second_depth"), rs.getString("third_depth"))
                    .filter(Objects::nonNull).map(String::strip).filter(part -> !part.isEmpty())
                    .collect(java.util.stream.Collectors.joining(" "));
            result.computeIfAbsent(rs.getLong("member_id"), ignored -> new ArrayList<>())
                    .add(new ExpertRegionResponse(rs.getLong("region_code"), name));
        });
        return result;
    }

    public Map<Long, String> findRepresentativeImages(List<Long> ids, Specialty specialty) {
        // 분야 우선, 생성 시각, ID 순으로 더 나은 후보가 없는 한 장만 조회한다.
        String sql = """
                SELECT i.member_id, i.image_key FROM expert_image i
                WHERE i.member_id IN (:ids) AND NOT EXISTS (
                    SELECT 1 FROM expert_image newer WHERE newer.member_id = i.member_id AND (
                        (CASE WHEN newer.specialty = :specialty THEN 1 ELSE 0 END)
                            > (CASE WHEN i.specialty = :specialty THEN 1 ELSE 0 END)
                        OR ((CASE WHEN newer.specialty = :specialty THEN 1 ELSE 0 END)
                            = (CASE WHEN i.specialty = :specialty THEN 1 ELSE 0 END)
                            AND (newer.created_at > i.created_at
                                 OR (newer.created_at = i.created_at AND newer.expert_image_id > i.expert_image_id)))
                    ))
                """;
        Map<Long, String> result = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("ids", ids)
                .addValue("specialty", specialty == null ? null : specialty.name(), java.sql.Types.VARCHAR), rs -> {
            result.put(rs.getLong("member_id"), rs.getString("image_key"));
        });
        return result;
    }
}
