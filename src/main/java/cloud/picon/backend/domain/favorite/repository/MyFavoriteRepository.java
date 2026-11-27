package cloud.picon.backend.domain.favorite.repository;

import cloud.picon.backend.domain.expert.repository.ExpertVisibility;
import cloud.picon.backend.global.apipayload.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MyFavoriteRepository {

    private static final String VISIBLE_FAVORITES = """
            FROM favorite f
            JOIN expert e ON e.member_id = f.expert_member_id
            JOIN member m ON m.member_id = e.member_id
            WHERE f.member_id = :memberId AND
            """ + ExpertVisibility.NICKNAME_CONDITION;

    private final NamedParameterJdbcTemplate jdbc;

    public record FavoriteRow(long expertId, String nickname, String profileImageKey, String introduction,
                              Integer startingPrice, BigDecimal rating, int reviewCount) {
    }

    @Transactional(readOnly = true)
    public PageResponse<FavoriteRow> findPage(long memberId, int page, int size) {
        var params = new MapSqlParameterSource("memberId", memberId)
                .addValue("nonWhitespacePattern", ExpertVisibility.NON_WHITESPACE_PATTERN);
        long total = jdbc.queryForObject("SELECT COUNT(*) " + VISIBLE_FAVORITES, params, Long.class);
        long offset = (long) page * size;
        if (offset >= total) {
            return PageResponse.of(List.of(), page, size, total);
        }

        var rows = jdbc.query("""
                SELECT e.member_id, m.nickname, m.profile_image, m.introduction, e.avg_rating, e.review_count,
                       (SELECT MIN(es.base_price) FROM expert_specialty es
                        WHERE es.member_id = e.member_id AND es.name IN ('SHOOTING', 'EDITING')
                          AND es.base_price >= 0) AS starting_price
                """ + VISIBLE_FAVORITES + "\n" + """
                ORDER BY f.created_at DESC, f.favorite_id DESC
                LIMIT :size OFFSET :offset
                """, params.addValue("size", size).addValue("offset", offset),
                (rs, index) -> new FavoriteRow(rs.getLong("member_id"), rs.getString("nickname"),
                        rs.getString("profile_image"), rs.getString("introduction"),
                        rs.getObject("starting_price", Integer.class), rs.getBigDecimal("avg_rating"),
                        rs.getInt("review_count")));
        return PageResponse.of(rows, page, size, total);
    }
}
