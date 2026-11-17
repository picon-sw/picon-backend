package cloud.picon.backend.domain.auth.controller;

import cloud.picon.backend.domain.auth.dto.KakaoLoginRequest;
import cloud.picon.backend.domain.auth.dto.LoginResponse;
import cloud.picon.backend.domain.auth.dto.LoginResult;
import cloud.picon.backend.domain.auth.dto.TokenRefreshResponse;
import cloud.picon.backend.domain.auth.dto.TokenRefreshResult;
import cloud.picon.backend.domain.auth.service.AuthService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import cloud.picon.backend.global.apipayload.code.ErrorCode;
import cloud.picon.backend.global.apipayload.exception.BusinessException;
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
        try {
            TokenRefreshResult result = authService.refresh(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(result.refreshToken()).toString())
                    .body(ApiResponse.onSuccess(result.response()));
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.INVALID_REFRESH_TOKEN) {
                throw e;
            }
            // 쓸 수 없는 쿠키가 브라우저에 남지 않도록 삭제하고 401 응답
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
                    .body(ApiResponse.onFailure(e.getErrorCode(), e.getErrorCode().getMessage(), null));
        }
    }
}