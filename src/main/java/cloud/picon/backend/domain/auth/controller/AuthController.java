package cloud.picon.backend.domain.auth.controller;

import cloud.picon.backend.domain.auth.dto.KakaoLoginRequest;
import cloud.picon.backend.domain.auth.dto.LoginResponse;
import cloud.picon.backend.domain.auth.dto.LoginResult;
import cloud.picon.backend.domain.auth.service.AuthService;
import cloud.picon.backend.global.apipayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
}