package cloud.picon.backend.domain.review.service;

import cloud.picon.backend.domain.review.code.ReviewErrorCode;
import cloud.picon.backend.domain.review.dto.ReviewCreateRequest;
import cloud.picon.backend.domain.review.dto.ReviewResponse;
import cloud.picon.backend.domain.review.repository.ReviewRepository;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class ReviewRegistrationService {

    private static final int MAX_CONTENT_CODE_POINTS = 2_000;
    private static final int MAX_TAGS_JSON_BYTES = 16 * 1024;

    private final ReviewRepository repository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewResponse register(long reservationId, long memberId, ReviewCreateRequest request) {
        var value = request.rating();
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()
                || value.intValue() < 1 || value.intValue() > 5) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        int rating = value.intValue();
        String content = request.reviewContent();
        // Even 2,000 four-byte UTF-8 code points fit within a MySQL TEXT column.
        if (content != null && content.codePointCount(0, content.length()) > MAX_CONTENT_CODE_POINTS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var tags = request.reviewTags();
        if (tags != null) {
            for (var tag : tags) {
                if (!tag.isString()) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
            }
        }
        String tagsJson = tags == null ? null : tags.toString();
        if (tagsJson != null && tagsJson.getBytes(StandardCharsets.UTF_8).length > MAX_TAGS_JSON_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!repository.lockReservation(reservationId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        var context = repository.findReservationContext(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (context.requesterMemberId() != memberId) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"COMPLETED".equals(context.reservationStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        if (context.reviewExists()) {
            throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
        if (!repository.lockExpert(context.expertId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        var now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
        long reviewId;
        try {
            reviewId = repository.insert(new ReviewRepository.NewReview(reservationId, rating,
                    tagsJson, content, now, now));
        } catch (DuplicateKeyException e) {
            if (repository.findReservationContext(reservationId)
                    .map(ReviewRepository.ReservationContext::reviewExists).orElse(false)) {
                throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
            }
            throw e;
        }
        repository.refreshExpertRating(context.expertId());
        return new ReviewResponse(reviewId, rating, tags, content, now.atOffset(ZoneOffset.UTC));
    }
}
