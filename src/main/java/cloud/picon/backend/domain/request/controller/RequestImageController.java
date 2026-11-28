package cloud.picon.backend.domain.request.controller;

import cloud.picon.backend.domain.request.dto.RequestImageUploadUrlRequest;
import cloud.picon.backend.domain.request.dto.RequestImageUploadUrlResponse;
import cloud.picon.backend.domain.request.service.RequestImageUploadService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.jwt.LoginMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests/images")
@RequiredArgsConstructor
public class RequestImageController implements RequestImageControllerDocs {

    private final RequestImageUploadService uploadService;

    @Override
    @PostMapping("/upload-urls")
    public ResponseEntity<ApiResponse<RequestImageUploadUrlResponse>> issueUploadUrls(
            @Valid @RequestBody RequestImageUploadUrlRequest request, @LoginMemberId Long memberId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(uploadService.issueUploadUrls(memberId, request)));
    }
}