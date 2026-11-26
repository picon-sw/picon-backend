package cloud.picon.backend.domain.review.controller;

import cloud.picon.backend.domain.review.dto.ReviewCreateRequest;
import cloud.picon.backend.domain.review.dto.ReviewResponse;
import cloud.picon.backend.domain.review.service.ExpertReviewQueryService;
import cloud.picon.backend.domain.review.service.ReviewRegistrationService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.code.SuccessCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.jwt.LoginMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController implements ReviewControllerDocs {

    private final ReviewRegistrationService registration;
    private final ExpertReviewQueryService query;

    @Override
    @PostMapping("/reservations/{reservationId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> register(@PathVariable long reservationId,
            @Valid @RequestBody ReviewCreateRequest request, @LoginMemberId Long memberId) {
        if (reservationId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ResponseEntity.status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.onSuccess(SuccessCode.CREATED, registration.register(reservationId, memberId, request)));
    }

    @Override
    @GetMapping("/experts/{expertId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> findReviews(@PathVariable long expertId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @LoginMemberId Long memberId) {
        if (expertId <= 0 || page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ResponseEntity.ok(ApiResponse.onSuccess(query.findReviews(expertId, page, size)));
    }
}
