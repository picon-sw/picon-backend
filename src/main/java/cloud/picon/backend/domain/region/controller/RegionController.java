package cloud.picon.backend.domain.region.controller;

import cloud.picon.backend.domain.region.dto.RegionResponse;
import cloud.picon.backend.domain.region.service.RegionService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController implements RegionControllerDocs {

    private final RegionService regionService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<RegionResponse>>> getRegions(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.onSuccess(regionService.findRegions(keyword)));
    }
}