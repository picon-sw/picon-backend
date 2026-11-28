package cloud.picon.backend.domain.request.controller;

import cloud.picon.backend.domain.request.dto.RequestImageUploadUrlRequest;
import cloud.picon.backend.domain.request.dto.RequestImageUploadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Requests", description = "의뢰")
public interface RequestImageControllerDocs {

    @Operation(summary = "의뢰 사진 업로드 URL 발급", security = @SecurityRequirement(name = "bearerAuth"), description = """
            의뢰 등록 전에 사진을 S3에 직접 올릴 수 있는 URL을 발급합니다.
            참고 사진(REFERENCE)은 최대 1장, 보정할 사진(TO_EDIT)은 최대 30장입니다.
            형식은 image/jpeg, image/png, image/webp이고, 크기는 장당 최대 10MB(10,485,760바이트)입니다.
            응답은 요청한 순서와 같습니다. uploadUrl로 PUT 요청을 보낼 때 Content-Type 헤더는 요청한 contentType과,
            파일 크기는 요청한 fileSize와 같아야 합니다. 다르면 S3가 거절합니다.
            imageKey는 의뢰 등록 API에 보내는 값이며, 발급받은 회원만 사용할 수 있습니다.
            expiresIn(초)이 지나면 URL을 다시 발급받아야 합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK: 발급 성공"),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: 사진 없음, 형식·크기·장수 초과",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INVALID_REQUEST", value = """
                            {"isSuccess":false,"code":"INVALID_REQUEST","message":"요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: 인증 누락 또는 유효하지 않은 토큰",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "UNAUTHORIZED", value = """
                            {"isSuccess":false,"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
                            """))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: 서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                            {"isSuccess":false,"code":"INTERNAL_ERROR","message":"서버 오류가 발생했습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<RequestImageUploadUrlResponse>> issueUploadUrls(
            RequestImageUploadUrlRequest request, @Parameter(hidden = true) Long memberId);
}