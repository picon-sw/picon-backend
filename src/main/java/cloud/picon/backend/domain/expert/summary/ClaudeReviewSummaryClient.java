package cloud.picon.backend.domain.expert.summary;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static cloud.picon.backend.domain.expert.summary.ReviewSummaryException.Kind.*;

class ClaudeReviewSummaryClient {

    private final RestClient client;
    private final ClaudeReviewSummaryProperties properties;

    ClaudeReviewSummaryClient(RestClient client, ClaudeReviewSummaryProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    String summarize(String prompt, String reviews) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new ReviewSummaryException(CONFIGURATION, "ANTHROPIC_API_KEY is required for review summarization");
        }

        var request = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "thinking", Map.of("type", "disabled"),
                "system", prompt,
                "messages", List.of(Map.of("role", "user", "content", reviews)),
                "output_config", Map.of("format", Map.of(
                        "type", "json_schema",
                        "schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "status", Map.of("type", "string", "enum", List.of("ok", "fail")),
                                        "summary_text", Map.of("type", "string")),
                                "required", List.of("status", "summary_text"),
                                "additionalProperties", false))));

        try {
            return client.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                    .header("anthropic-version", properties.apiVersion())
                    .body(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (sent, response) -> { throw httpError(response.getStatusCode().value()); })
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw httpError(e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException) {
                    throw new ReviewSummaryException(TIMEOUT, "Claude request timed out");
                }
            }
            throw new ReviewSummaryException(TRANSPORT, "Claude communication failed");
        } catch (RestClientException e) {
            throw new ReviewSummaryException(INVALID_RESPONSE, "Claude response could not be read");
        }
    }

    private ReviewSummaryException httpError(int status) {
        var kind = switch (status) {
            case 401, 403 -> AUTHENTICATION;
            case 429 -> RATE_LIMIT;
            default -> status >= 500 ? PROVIDER : HTTP;
        };
        return new ReviewSummaryException(kind, "Claude returned HTTP " + status);
    }
}
