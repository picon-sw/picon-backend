package cloud.picon.backend.domain.expert.summary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "PICON_CLAUDE_LIVE_TEST", matches = "true")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = "(?s).*\\S.*")
@EnabledIfEnvironmentVariable(named = "CLAUDE_REVIEW_SUMMARY_PROMPT", matches = "(?s).*\\S.*")
class ClaudeReviewSummaryLiveTest {

    private static final String SETTINGS = "app.claude.review-summary.";
    private static final URI ANTHROPIC_URL = URI.create("https://api.anthropic.com");

    @Test
    void summarizesSevenSyntheticReviewsInOneRequest() throws IOException {
        var properties = liveProperties();
        var mapper = JsonMapper.builder().build();
        var diagnostics = new CallDiagnostics();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        var restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(factory)
                .bufferContent((uri, method) -> true)
                .requestInterceptor((request, body, execution) -> {
                    assertThat(++diagnostics.requests).as("Claude request count").isEqualTo(1);
                    long started = System.nanoTime();
                    try {
                        var response = execution.execute(request, body);
                        diagnostics.httpStatus = response.getStatusCode().value();
                        String requestId = response.getHeaders().getFirst("request-id");
                        if (requestId != null && requestId.matches("req_[A-Za-z0-9_-]{1,120}")) {
                            diagnostics.requestId = requestId;
                        }
                        if (response.getStatusCode().is2xxSuccessful()) {
                            try {
                                JsonNode usage = mapper.readTree(response.getBody().readAllBytes()).path("usage");
                                diagnostics.inputTokens = usage.path("input_tokens").asInt(-1);
                                diagnostics.outputTokens = usage.path("output_tokens").asInt(-1);
                            } catch (JacksonException ignored) {
                                // The service validates the same response and reports invalid JSON.
                            } catch (IOException e) {
                                response.close();
                                throw e;
                            }
                        }
                        return response;
                    } finally {
                        diagnostics.elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    }
                })
                .build();
        var client = new ClaudeReviewSummaryClient(restClient, properties);
        var service = new ReviewSummaryService(client, properties, mapper);

        try {
            // The service checks end_turn, the exact JSON fields and types, and the three-sentence limit.
            var result = service.summarize(List.of(
                    "촬영 전에 원하는 분위기를 물어봐 줬어요.",
                    "포즈를 알려줘서 긴장이 풀렸어요.",
                    "촬영 내내 편안했어요.",
                    "사진 색감이 자연스러웠어요.",
                    "보정본을 약속한 날 받았어요.",
                    "보정본이 예상보다 늦었어요.",
                    "감사합니다."));
            assertThat(diagnostics.requests).isEqualTo(1);
            assertThat(diagnostics.httpStatus).isEqualTo(200);
            assertThat(result.status()).isEqualTo("ok");
            assertThat(result.summaryText()).isNotBlank();
            assertThat(result.summaryText().codePointCount(0, result.summaryText().length())).isLessThanOrEqualTo(150);

            // Only show the validated result of the fixed synthetic reviews above.
            assertThat(result.summaryText().contains(properties.apiKey()))
                    .as("Summary contains the API key").isFalse();
            System.out.println("Claude synthetic review result: " + mapper.writeValueAsString(result));
        } catch (ReviewSummaryException e) {
            diagnostics.errorKind = e.kind().name();
            fail("Claude live test failed: " + diagnostics.errorKind);
        } finally {
            diagnostics.report();
        }
    }

    private static ClaudeReviewSummaryProperties liveProperties() throws IOException {
        var environment = new StandardEnvironment();
        var source = new YamlPropertySourceLoader().load("claude-live-defaults",
                new ClassPathResource("application.yaml")).getFirst();
        environment.getPropertySources().addFirst(source);
        var binder = Binder.get(environment);
        URI baseUrl = URI.create(environment.getRequiredProperty(SETTINGS + "base-url"));
        assertThat(baseUrl.equals(ANTHROPIC_URL)).as("Official Anthropic endpoint").isTrue();

        FileSystemResource prompt;
        try {
            prompt = new FileSystemResource(Path.of(URI.create(System.getenv("CLAUDE_REVIEW_SUMMARY_PROMPT"))));
        } catch (IllegalArgumentException e) {
            return fail("CLAUDE_REVIEW_SUMMARY_PROMPT must be an absolute file URI");
        }
        assertThat(prompt.isFile() && prompt.isReadable()).as("Readable prompt file").isTrue();

        return new ClaudeReviewSummaryProperties(baseUrl, System.getenv("ANTHROPIC_API_KEY"),
                environment.getRequiredProperty(SETTINGS + "model"),
                environment.getRequiredProperty(SETTINGS + "api-version"),
                environment.getRequiredProperty(SETTINGS + "max-tokens", Integer.class),
                binder.bind(SETTINGS + "connect-timeout", Duration.class).get(),
                binder.bind(SETTINGS + "read-timeout", Duration.class).get(), prompt);
    }

    private static final class CallDiagnostics {
        private int requests;
        private int httpStatus;
        private long elapsedMs;
        private int inputTokens = -1;
        private int outputTokens = -1;
        private String requestId = "unavailable";
        private String errorKind = "none";

        private void report() {
            System.out.printf("Claude live: requests=%d elapsed_ms=%d http_status=%s request_id=%s "
                            + "input_tokens=%s output_tokens=%s error_kind=%s%n",
                    requests, elapsedMs, httpStatus == 0 ? "unavailable" : httpStatus, requestId,
                    inputTokens < 0 ? "unavailable" : inputTokens,
                    outputTokens < 0 ? "unavailable" : outputTokens, errorKind);
        }
    }
}
