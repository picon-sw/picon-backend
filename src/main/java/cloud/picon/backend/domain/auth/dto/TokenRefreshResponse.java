package cloud.picon.backend.domain.auth.dto;

public record TokenRefreshResponse(
        String accessToken,
        long expiresIn
) {
}