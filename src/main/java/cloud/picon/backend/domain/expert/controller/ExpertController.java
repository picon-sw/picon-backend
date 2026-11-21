package cloud.picon.backend.domain.expert.controller;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.service.ExpertDetailService;
import cloud.picon.backend.domain.expert.service.ExpertImageService;
import cloud.picon.backend.domain.expert.service.ExpertService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.PageResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import cloud.picon.backend.global.jwt.LoginMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class ExpertController implements ExpertControllerDocs {

    private final ExpertService expertService;
    private final ExpertDetailService expertDetailService;
    private final ExpertImageService expertImageService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpertResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Specialty specialty,
            @RequestParam(required = false) Long photoStyleId,
            @RequestParam(required = false) Long regionCode,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minCareerYears,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @LoginMemberId Long memberId) {
        ExpertSearchCondition condition = new ExpertSearchCondition(keyword, specialty, photoStyleId, regionCode,
                minPrice, maxPrice, minCareerYears, ExpertSort.from(sort == null ? "rating" : sort), page, size);
        return ResponseEntity.ok(ApiResponse.onSuccess(expertService.search(condition, memberId)));
    }

    @Override
    @GetMapping("/{expertId}")
    public ResponseEntity<ApiResponse<ExpertDetailResponse>> getDetail(
            @PathVariable Long expertId, @LoginMemberId Long memberId) {
        if (expertId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ResponseEntity.ok(ApiResponse.onSuccess(expertDetailService.getDetail(expertId, memberId)));
    }

    @Override
    @GetMapping("/{expertId}/images")
    public ResponseEntity<ApiResponse<PageResponse<ExpertImageResponse>>> getImages(
            @PathVariable Long expertId,
            @RequestParam(required = false) Specialty specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @LoginMemberId Long memberId) {
        if (expertId <= 0 || page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ResponseEntity.ok(ApiResponse.onSuccess(expertImageService.getImages(expertId, specialty, page, size)));
    }
}
