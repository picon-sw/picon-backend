package cloud.picon.backend.domain.auth.dto;

public record TokenRefreshResult(
        TokenRefreshResponse response,
        String refreshToken
) {
}