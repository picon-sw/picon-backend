package cloud.picon.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        String accessToken,
        long expiresIn,
        @JsonProperty("isNewMember") boolean isNewMember,
        @JsonProperty("isOnboarded") boolean isOnboarded
) {
}