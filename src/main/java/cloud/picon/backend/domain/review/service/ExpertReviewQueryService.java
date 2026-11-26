package cloud.picon.backend.domain.review.service;

import cloud.picon.backend.domain.expert.code.ExpertErrorCode;
import cloud.picon.backend.domain.expert.repository.ExpertDetailRepository;
import cloud.picon.backend.domain.review.dto.ReviewResponse;
import cloud.picon.backend.domain.review.repository.ReviewRepository;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

@Service
@RequiredArgsConstructor
public class ExpertReviewQueryService {

    private final ExpertDetailRepository experts;
    private final ReviewRepository reviews;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> findReviews(long expertId, int page, int size) {
        if (!experts.existsVisibleExpert(expertId)) {
            throw new BusinessException(ExpertErrorCode.EXPERT_NOT_FOUND);
        }
        long total = reviews.countByExpertId(expertId);
        var content = reviews.findPage(expertId, size, (long) page * size).stream()
                .map(row -> new ReviewResponse(row.reviewId(), row.rating(), readTags(row.tagsJson()),
                        row.content(), row.createdAt()))
                .toList();
        return PageResponse.of(content, page, size, total);
    }

    private ArrayNode readTags(String json) {
        if (json == null) {
            return null;
        }
        if (jsonMapper.readTree(json) instanceof ArrayNode tags) {
            return tags;
        }
        throw new IllegalStateException("Stored review tags must be a JSON array");
    }
}
