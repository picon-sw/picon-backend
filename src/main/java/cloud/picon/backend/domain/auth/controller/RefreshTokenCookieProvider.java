package cloud.picon.backend.domain.auth.controller;

import cloud.picon.backend.global.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final JwtProperties jwtProperties;

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken)
                .maxAge(jwtProperties.refreshTokenExpiration())
                .build();
    }

    // 이름·경로가 같은 쿠키를 Max-Age=0으로 보내면 브라우저가 기존 쿠키를 지움
    public ResponseCookie expire() {
        return base("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }
}