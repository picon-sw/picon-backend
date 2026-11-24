package cloud.picon.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenRefreshResponse(
        String accessToken,
        long expiresIn,
        @JsonProperty("isOnboarded") boolean isOnboarded
) {
}