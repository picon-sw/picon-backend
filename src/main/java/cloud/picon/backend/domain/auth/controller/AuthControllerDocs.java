package cloud.picon.backend.domain.auth.controller;

import cloud.picon.backend.domain.auth.dto.KakaoLoginRequest;
import cloud.picon.backend.domain.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "로그인, 토큰")
public interface AuthControllerDocs {

    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 인가 코드로 로그인합니다. 처음 로그인하면 회원을 자동으로 생성합니다.
                    Refresh Token은 응답 본문이 아니라 Set-Cookie(HttpOnly)로 전달합니다.
                    """)
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "code 또는 redirectUri 누락, 허용되지 않은 redirectUri",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess": false, "code": "INVALID_REQUEST", "message": "요청 값이 올바르지 않습니다."}
                            """))),
            @ApiResponse(responseCode = "401", description = "인가 코드가 만료됐거나 이미 사용됨",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess": false, "code": "KAKAO_AUTH_FAILED", "message": "카카오 인증에 실패했습니다."}
                            """))),
            @ApiResponse(responseCode = "502", description = "카카오 서버 응답 실패",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"isSuccess": false, "code": "KAKAO_SERVER_ERROR", "message": "카카오 서버와 통신하지 못했습니다."}
                            """)))
    })
    ResponseEntity<cloud.picon.backend.global.apipayload.ApiResponse<LoginResponse>> kakaoLogin(
            KakaoLoginRequest request);
}