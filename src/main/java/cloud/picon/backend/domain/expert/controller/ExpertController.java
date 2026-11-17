package cloud.picon.backend.domain.expert.controller;

import cloud.picon.backend.domain.expert.dto.*;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.service.ExpertService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
public class ExpertController implements ExpertControllerDocs {

    private final ExpertService expertService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<ExpertPageResponse>> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Specialty specialty,
            @RequestParam(required = false) Long photoStyleId,
            @RequestParam(required = false) Long regionCode,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minCareerYears,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        long memberId = memberId(jwt);
        ExpertSearchCondition condition = new ExpertSearchCondition(keyword, specialty, photoStyleId, regionCode,
                minPrice, maxPrice, minCareerYears, ExpertSort.from(sort == null ? "rating" : sort), page, size);
        return ResponseEntity.ok(ApiResponse.onSuccess(expertService.search(condition, memberId)));
    }

    private long memberId(Jwt jwt) {
        try {
            String subject = jwt == null ? null : jwt.getSubject();
            if (subject == null || !subject.matches("[0-9]+")) {
                throw new IllegalArgumentException();
            }
            long memberId = Long.parseLong(subject);
            if (memberId <= 0) {
                throw new IllegalArgumentException();
            }
            return memberId;
        } catch (IllegalArgumentException | ClassCastException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
