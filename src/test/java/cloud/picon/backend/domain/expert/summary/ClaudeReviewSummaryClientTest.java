package cloud.picon.backend.domain.expert.summary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static cloud.picon.backend.domain.expert.summary.ReviewSummaryException.Kind.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClaudeReviewSummaryClientTest {

    private static final String BASE_URL = "https://claude.example.invalid";
    private static final String PROMPT = "\uB9AC\uBDF0\uB97C \uC694\uC57D\uD574\uC8FC\uC138\uC694.\r\n";
    private final JsonMapper mapper = JsonMapper.builder().build();
    @TempDir
    Path tempDir;
    private Path promptFile;
    private MockRestServiceServer server;
    private ReviewSummaryService service;

    @BeforeEach
    void setUp() throws IOException {
        promptFile = Files.writeString(tempDir.resolve("prompt.txt"), PROMPT);
        service = serviceWithKey("mock-key");
    }

    @Test
    void sendsOneRequestWithAllReviewsAndParsesSuccess() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-key"))
                .andExpect(header("anthropic-version", "2026-10-21"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("claude-haiku-5-5"))
                .andExpect(jsonPath("$.max_tokens").value(256))
                .andExpect(jsonPath("$.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.system").value(PROMPT))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Great photos. Prompt delivery.\nFriendly photographer."))
                .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
                .andRespond(withSuccess(message("""
                        {"status":"ok","summary_text":"Great photos and prompt delivery."}
                        """), MediaType.APPLICATION_JSON));

        assertThat(service.summarize(List.of("Great photos.\r\nPrompt delivery.", "Friendly photographer.")))
                .isEqualTo(new ReviewSummaryResult("ok", "Great photos and prompt delivery."));
        server.verify();
    }

    @Test
    void returnsModelFailAsAResult() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withSuccess(message("""
                        {"status":"fail","summary_text":""}
                        """), MediaType.APPLICATION_JSON));
        assertThat(service.summarize(List.of("Thanks"))).isEqualTo(new ReviewSummaryResult("fail", ""));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 529})
    void classifiesHttpErrors(int status) {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)).body("sensitive upstream body"));
        var expected = switch (status) {
            case 401, 403 -> AUTHENTICATION;
            case 429 -> RATE_LIMIT;
            case 500, 529 -> PROVIDER;
            default -> HTTP;
        };
        assertFailure(expected);
    }

    @Test
    void classifiesTimeout() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withException(new SocketTimeoutException("mock")));
        assertFailure(TIMEOUT);
    }

    @Test
    void classifiesCommunicationFailure() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withException(new IOException("mock")));
        assertFailure(TRANSPORT);
    }

    @Test
    void rejectsMissingKeyBeforeSending() {
        service = serviceWithKey("");
        assertFailure(CONFIGURATION);
    }

    @Test
    void rejectsMissingPromptBeforeSending() throws IOException {
        Files.delete(promptFile);
        assertFailure(CONFIGURATION);
    }

    private void assertFailure(ReviewSummaryException.Kind kind) {
        assertThatThrownBy(() -> service.summarize(List.of("Great photos")))
                .isInstanceOfSatisfying(ReviewSummaryException.class, exception -> {
                    assertThat(exception.kind()).isEqualTo(kind);
                    assertThat(exception.getMessage()).doesNotContain("mock-key", "sensitive upstream body");
                    assertThat(exception.getCause()).isNull();
                });
        server.verify();
    }

    private ReviewSummaryService serviceWithKey(String key) {
        var properties = new ClaudeReviewSummaryProperties(URI.create(BASE_URL), key, "claude-haiku-5-5",
                "2026-10-21", 256, Duration.ofSeconds(3), Duration.ofSeconds(30),
                new FileSystemResource(promptFile));
        var builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        return new ReviewSummaryService(new ClaudeReviewSummaryClient(builder.build(), properties), properties, mapper);
    }

    private String message(String output) {
        return mapper.writeValueAsString(Map.of("stop_reason", "end_turn", "content",
                List.of(Map.of("type", "text", "text", output))));
    }
}
