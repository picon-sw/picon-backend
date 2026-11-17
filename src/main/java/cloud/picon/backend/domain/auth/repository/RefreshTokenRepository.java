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

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    public void save(String refreshToken, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(key(refreshToken), String.valueOf(memberId), ttl);
    }

    // 조회와 삭제를 한 번에 처리(Redis GETDEL). 같은 토큰으로 동시에 재발급해도 한 요청만 성공
    public Optional<Long> findMemberIdAndDelete(String refreshToken) {
        String memberId = redisTemplate.opsForValue().getAndDelete(key(refreshToken));
        return Optional.ofNullable(memberId).map(Long::valueOf);
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(key(refreshToken));
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + sha256(refreshToken);
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