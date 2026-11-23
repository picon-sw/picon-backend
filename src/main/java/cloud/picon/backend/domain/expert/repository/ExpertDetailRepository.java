package cloud.picon.backend.domain.expert.repository;

import cloud.picon.backend.domain.expert.dto.ExpertPhotoStyleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExpertDetailRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record ExpertDetailRow(Long id, String nickname, String profileImageKey, String introduction,
                                  int careerYears, BigDecimal rating, int reviewCount,
                                  String reviewSummary, boolean favorite) {
    }

    public Optional<ExpertDetailRow> findExpert(long expertId, long memberId) {
        return jdbc.query("""
                SELECT e.member_id, m.nickname, m.profile_image, m.introduction,
                       e.career_years, e.avg_rating, e.review_count, e.review_summary,
                       EXISTS (SELECT 1 FROM favorite f WHERE f.member_id = :viewerId
                               AND f.expert_member_id = e.member_id) AS favorite
                FROM expert e JOIN member m ON m.member_id = e.member_id
                WHERE e.member_id = :expertId AND m.nickname IS NOT NULL
                  AND REGEXP_LIKE(m.nickname, :nonWhitespacePattern)
                """, new MapSqlParameterSource("expertId", expertId).addValue("viewerId", memberId)
                        .addValue("nonWhitespacePattern", ExpertSearchRepository.NON_WHITESPACE_PATTERN),
                (rs, index) -> new ExpertDetailRow(rs.getLong("member_id"), rs.getString("nickname"),
                        rs.getString("profile_image"), rs.getString("introduction"), rs.getInt("career_years"),
                        rs.getBigDecimal("avg_rating"), rs.getInt("review_count"), rs.getString("review_summary"),
                        rs.getBoolean("favorite"))).stream().findFirst();
    }

    public List<ExpertPhotoStyleResponse> findPhotoStyles(long expertId) {
        return jdbc.query("""
                SELECT DISTINCT ps.photo_style_id, ps.name
                FROM expert_photo_style eps JOIN photo_style ps ON ps.photo_style_id = eps.photo_style_id
                WHERE eps.member_id = :expertId ORDER BY ps.photo_style_id
                """, new MapSqlParameterSource("expertId", expertId),
                (rs, index) -> new ExpertPhotoStyleResponse(rs.getLong("photo_style_id"), rs.getString("name")));
    }

    public boolean existsVisibleExpert(long expertId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM expert e JOIN member m ON m.member_id = e.member_id
                    WHERE e.member_id = :expertId AND m.nickname IS NOT NULL
                      AND REGEXP_LIKE(m.nickname, :nonWhitespacePattern)
                )
                """, new MapSqlParameterSource("expertId", expertId)
                .addValue("nonWhitespacePattern", ExpertSearchRepository.NON_WHITESPACE_PATTERN), Boolean.class));
    }
}
