package cloud.picon.backend.domain.review.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record ReservationContext(long reservationId, long proposalId, long requestId,
                                     long requesterMemberId, long expertId, String reservationStatus,
                                     boolean reviewExists) {
    }

    public record ReviewBody(long reviewId, String content) {
    }

    public record NewReview(long reservationId, int rating, String tagsJson,
                            String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record ReviewRow(long reviewId, int rating, String tagsJson, String content, LocalDateTime createdAt) {
    }

    private static final String EXPERT_REVIEWS = """
            FROM review v
            JOIN reservation r ON r.reservation_id = v.reservation_id
            JOIN proposal p ON p.proposal_id = r.proposal_id
            WHERE p.member_id = :expertId
            """;

    public boolean lockReservation(long reservationId) {
        return !jdbc.queryForList("""
                SELECT reservation_id FROM reservation WHERE reservation_id = :id FOR UPDATE
                """, Map.of("id", reservationId), Long.class).isEmpty();
    }

    public boolean lockExpert(long expertId) {
        return !jdbc.queryForList("""
                SELECT member_id FROM expert WHERE member_id = :id FOR UPDATE
                """, Map.of("id", expertId), Long.class).isEmpty();
    }

    public Optional<ReservationContext> findReservationContext(long reservationId) {
        return jdbc.query("""
                SELECT r.reservation_id, p.proposal_id, q.request_id, q.member_id AS requester_member_id,
                       e.member_id AS expert_id, r.status AS reservation_status,
                       EXISTS (SELECT 1 FROM review v WHERE v.reservation_id = r.reservation_id) AS review_exists
                FROM reservation r
                JOIN proposal p ON p.proposal_id = r.proposal_id
                JOIN `request` q ON q.request_id = p.request_id
                JOIN expert e ON e.member_id = p.member_id
                WHERE r.reservation_id = :reservationId
                """, Map.of("reservationId", reservationId), (rs, rowNum) -> new ReservationContext(
                rs.getLong("reservation_id"), rs.getLong("proposal_id"), rs.getLong("request_id"),
                rs.getLong("requester_member_id"), rs.getLong("expert_id"), rs.getString("reservation_status"),
                rs.getBoolean("review_exists"))).stream().findFirst();
    }

    public List<ReviewBody> findBodiesByExpertId(long expertId) {
        return jdbc.query("""
                SELECT v.review_id, v.review_content
                FROM review v
                JOIN reservation r ON r.reservation_id = v.reservation_id
                JOIN proposal p ON p.proposal_id = r.proposal_id
                JOIN expert e ON e.member_id = p.member_id
                WHERE e.member_id = :expertId
                ORDER BY v.review_id
                """, Map.of("expertId", expertId),
                (rs, rowNum) -> new ReviewBody(rs.getLong("review_id"), rs.getString("review_content")));
    }

    public long insert(NewReview review) {
        var key = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO review (reservation_id, rating, review_tags, review_content,
                                    created_at, updated_at)
                VALUES (:reservationId, :rating, :tags, :content, :createdAt, :updatedAt)
                """, new MapSqlParameterSource("reservationId", review.reservationId())
                .addValue("rating", review.rating())
                .addValue("tags", review.tagsJson(), Types.VARCHAR)
                .addValue("content", review.content(), Types.VARCHAR)
                .addValue("createdAt", review.createdAt())
                .addValue("updatedAt", review.updatedAt()), key, new String[]{"review_id"});
        return Objects.requireNonNull(key.getKey(), "Review insert did not return a generated ID").longValue();
    }

    public List<ReviewRow> findPage(long expertId, int size, long offset) {
        return jdbc.query("""
                SELECT v.review_id, v.rating, v.review_tags, v.review_content, v.created_at
                """ + EXPERT_REVIEWS + """
                ORDER BY v.created_at DESC, v.review_id DESC
                LIMIT :size OFFSET :offset
                """, Map.of("expertId", expertId, "size", size, "offset", offset),
                (rs, rowNum) -> new ReviewRow(rs.getLong("review_id"), rs.getInt("rating"),
                        rs.getString("review_tags"), rs.getString("review_content"),
                        rs.getObject("created_at", LocalDateTime.class)));
    }

    public long countByExpertId(long expertId) {
        return jdbc.queryForObject("SELECT COUNT(*) " + EXPERT_REVIEWS, Map.of("expertId", expertId), Long.class);
    }

    public void refreshExpertRating(long expertId) {
        var params = Map.of("expertId", expertId);
        var stats = jdbc.queryForObject("SELECT COUNT(*), ROUND(AVG(v.rating), 1) " + EXPERT_REVIEWS,
                params, (rs, rowNum) -> new MapSqlParameterSource("expertId", expertId)
                        .addValue("count", rs.getLong(1)).addValue("rating", rs.getBigDecimal(2)));
        jdbc.update("""
                UPDATE expert SET review_count = :count, avg_rating = :rating WHERE member_id = :expertId
                """, Objects.requireNonNull(stats));
    }
}
