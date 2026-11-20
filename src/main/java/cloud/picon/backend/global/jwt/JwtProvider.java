package cloud.picon.backend.global.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // {세션 ID 22자}.{무작위 43자}: 16바이트·32바이트를 Base64 URL(패딩 없음)로 표현한 길이
    private static final Pattern REFRESH_TOKEN_PATTERN =
            Pattern.compile("^([A-Za-z0-9_-]{22})\\.[A-Za-z0-9_-]{43}$");

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String createAccessToken(Long memberId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenExpiration()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String createSessionId() {
        return randomBase64Url(16);
    }

    public String createRefreshToken(String sessionId) {
        return sessionId + "." + randomBase64Url(32);
    }

    // 형식이 맞지 않으면 빈 값
    public Optional<String> extractSessionId(String refreshToken) {
        Matcher matcher = REFRESH_TOKEN_PATTERN.matcher(refreshToken);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public long getAccessTokenExpiresIn() {
        return jwtProperties.accessTokenExpiration().toSeconds();
    }

    private String randomBase64Url(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}