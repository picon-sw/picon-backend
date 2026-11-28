package cloud.picon.backend.domain.expert.summary;

import cloud.picon.backend.domain.review.repository.ReviewRepository;
import cloud.picon.backend.domain.review.repository.ReviewRepository.ReviewBody;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ExpertReviewSummaryStore {

    private static final int MAX_REVIEWS = 30;
    private static final int PAGE_SIZE = 100;

    private final ReviewRepository reviews;
    private final NamedParameterJdbcTemplate jdbc;

    record SummaryState(String text, int count, LocalDateTime updatedAt) {
    }

    record Snapshot(SummaryState existing, List<ReviewBody> input) {
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<Snapshot> snapshot(long expertId) {
        if (!reviews.lockExpert(expertId)) {
            return Optional.empty();
        }
        return Optional.of(readSnapshot(expertId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean saveIfCurrent(long expertId, Snapshot expected, String text) {
        if (!reviews.lockExpert(expertId) || !expected.equals(readSnapshot(expertId))) {
            return false;
        }

        return jdbc.update("""
                UPDATE expert
                SET review_summary = :text, review_summary_count = :count, review_summary_updated_at = :now
                WHERE member_id = :id
                """, Map.of("id", expertId, "text", text, "count", expected.input().size(),
                "now", LocalDateTime.now(ZoneOffset.UTC).withNano(0))) == 1;
    }

    private Snapshot readSnapshot(long expertId) {
        SummaryState existing = jdbc.queryForObject("""
                SELECT review_summary, review_summary_count, review_summary_updated_at
                FROM expert WHERE member_id = :id
                """, Map.of("id", expertId), (rs, index) -> new SummaryState(rs.getString("review_summary"),
                rs.getInt("review_summary_count"), rs.getObject("review_summary_updated_at", LocalDateTime.class)));

        var input = new ArrayList<ReviewBody>(MAX_REVIEWS);
        for (long offset = 0; input.size() < MAX_REVIEWS; offset += PAGE_SIZE) {
            var page = reviews.findNewestBodiesByExpertId(expertId, PAGE_SIZE, offset);
            for (var review : page) {
                if (ReviewSummaryInputPolicy.canSend(review.content())) {
                    input.add(review);
                    if (input.size() == MAX_REVIEWS) {
                        break;
                    }
                }
            }
            if (page.size() < PAGE_SIZE) {
                break;
            }
        }
        return new Snapshot(existing, List.copyOf(input));
    }
}
