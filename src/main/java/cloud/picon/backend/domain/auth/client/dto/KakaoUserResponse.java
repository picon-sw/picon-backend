package cloud.picon.backend.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(
        Long id
) {
}