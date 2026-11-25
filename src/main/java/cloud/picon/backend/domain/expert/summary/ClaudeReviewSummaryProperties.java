package cloud.picon.backend.domain.expert.summary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("app.claude.review-summary")
record ClaudeReviewSummaryProperties(
        URI baseUrl, String apiKey, String model, String apiVersion, int maxTokens,
        Duration connectTimeout, Duration readTimeout, Resource prompt
) {
    @Override
    public String toString() {
        return "ClaudeReviewSummaryProperties";
    }
}
