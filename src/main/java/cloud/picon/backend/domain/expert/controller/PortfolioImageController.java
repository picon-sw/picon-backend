package cloud.picon.backend.domain.expert.controller;

import cloud.picon.backend.domain.expert.dto.PortfolioImagePageResponse;
import cloud.picon.backend.domain.expert.entity.Specialty;
import cloud.picon.backend.domain.expert.service.PortfolioImageService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio-images")
@RequiredArgsConstructor
public class PortfolioImageController implements PortfolioImageControllerDocs {

    private final PortfolioImageService portfolioImageService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioImagePageResponse>> getPortfolioImages(
            @RequestParam(required = false) Specialty specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam MultiValueMap<String, String> parameters) {
        if (parameters.containsKey("tag") || page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ResponseEntity.ok(ApiResponse.onSuccess(
                portfolioImageService.getPortfolioImages(specialty, page, size)));
    }
}
