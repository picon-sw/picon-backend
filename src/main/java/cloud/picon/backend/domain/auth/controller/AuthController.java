package cloud.picon.backend.domain.auth.controller;

import cloud.picon.backend.domain.auth.dto.KakaoLoginRequest;
import cloud.picon.backend.domain.auth.dto.LoginResponse;
import cloud.picon.backend.domain.auth.dto.LoginResult;
import cloud.picon.backend.domain.auth.service.AuthService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.jwt.JwtProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Override
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        LoginResult result = authService.kakaoLogin(request);

        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(jwtProperties.refreshTokenExpiration())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.onSuccess(result.response()));
    }
}