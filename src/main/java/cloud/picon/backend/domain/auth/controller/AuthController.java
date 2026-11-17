package cloud.picon.backend.domain.auth.controller;

import cloud.picon.backend.domain.auth.dto.KakaoLoginRequest;
import cloud.picon.backend.domain.auth.dto.LoginResponse;
import cloud.picon.backend.domain.auth.dto.LoginResult;
import cloud.picon.backend.domain.auth.dto.TokenRefreshResponse;
import cloud.picon.backend.domain.auth.dto.TokenRefreshResult;
import cloud.picon.backend.domain.auth.service.AuthService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @Override
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        LoginResult result = authService.kakaoLogin(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
                .body(ApiResponse.onSuccess(result.response()));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
        // 실패 시 쿠키를 지우지 않음: 동시 재발급에서 늦게 도착한 실패 응답이 새 쿠키를 지우는 것을 막기 위함
        TokenRefreshResult result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
                .body(ApiResponse.onSuccess(result.response()));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
                .body(ApiResponse.onSuccess(null));
    }
}