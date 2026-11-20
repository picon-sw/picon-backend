package cloud.picon.backend.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String SESSION_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;

    public void save(String refreshToken, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(tokenKey(refreshToken), String.valueOf(memberId), ttl);
    }

    // 조회와 삭제를 한 번에 처리(Redis GETDEL). 같은 토큰으로 동시에 재발급해도 한 요청만 성공
    public Optional<Long> findMemberIdAndDelete(String refreshToken) {
        String memberId = redisTemplate.opsForValue().getAndDelete(tokenKey(refreshToken));
        return Optional.ofNullable(memberId).map(Long::valueOf);
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(tokenKey(refreshToken));
    }

    // 로그인 시 한 번만 저장. 연장하지 않으므로 ttl이 최대 로그인 유지 기간
    public void saveSession(String sessionId, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(sessionKey(sessionId), String.valueOf(memberId), ttl);
    }

    // 세션이 살아 있고 회원이 일치하면 남은 수명을 반환
    public Optional<Duration> findSessionRemaining(String sessionId, Long memberId) {
        String key = sessionKey(sessionId);
        String owner = redisTemplate.opsForValue().get(key);
        if (!String.valueOf(memberId).equals(owner)) {
            return Optional.empty();
        }
        Long remainingMillis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        if (remainingMillis == null || remainingMillis <= 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(remainingMillis));
    }

    public boolean existsSession(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)));
    }

    // 없는 세션이면 아무것도 생기지 않음 (임의 세션 ID로 키가 쌓이지 않음)
    public void deleteSession(String sessionId) {
        redisTemplate.delete(sessionKey(sessionId));
    }

    private String tokenKey(String refreshToken) {
        return TOKEN_PREFIX + sha256(refreshToken);
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sha256(sessionId);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}