package cloud.picon.backend.domain.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.kakao")
public record KakaoProperties(
        String clientId,
        String clientSecret,
        List<String> allowedRedirectUris
) {
}