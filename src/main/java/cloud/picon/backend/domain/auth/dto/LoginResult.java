package cloud.picon.backend.domain.auth.dto;

public record LoginResult(
        LoginResponse response,
        String refreshToken
) {
}