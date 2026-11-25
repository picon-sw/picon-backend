package cloud.picon.backend.domain.expert.summary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cloud.picon.backend.domain.expert.summary.ReviewSummaryException.Kind.INVALID_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReviewSummaryServiceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ClaudeReviewSummaryClient client = mock(ClaudeReviewSummaryClient.class);
    private final ReviewSummaryService service = new ReviewSummaryService(client,
            new ClaudeReviewSummaryProperties(null, "", "", "", 0, null, null,
                    new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8))), mapper);

    @Test
    void handlesEmptyAndBlankInputWithoutCallingClaude() {
        var fail = new ReviewSummaryResult("fail", "");
        assertThat(service.summarize(List.of())).isEqualTo(fail);
        assertThat(service.summarize(List.of(" \t", "\u00a0\n"))).isEqualTo(fail);
        assertThatIllegalArgumentException().isThrownBy(() -> service.summarize(List.of("Review", " ")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.summarize(Arrays.asList("Review", null)));
        assertThatIllegalArgumentException().isThrownBy(() -> service.summarize(null));
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "{", "{\"status\":\"fail\"}", "{\"summary_text\":\"\"}",
            "{\"status\":\"other\",\"summary_text\":\"\"}",
            "{\"status\":\"ok\",\"summary_text\":123}",
            "{\"status\":\"ok\",\"summary_text\":\" \"}",
            "{\"status\":\"fail\",\"summary_text\":\" \"}",
            "{\"status\":\"ok\",\"status\":\"fail\",\"summary_text\":\"\"}",
            "{\"status\":\"fail\",\"summary_text\":\"\"} {}",
            "{\"status\":\"fail\",\"summary_text\":\"\",\"extra\":1}"
    })
    void rejectsInvalidOutputJson(String output) {
        returnsOutput(output, "end_turn");
        assertInvalidResponse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"max_tokens", "refusal", "stop_sequence"})
    void rejectsUnexpectedStopReason(String stopReason) {
        returnsOutput("{\"status\":\"fail\",\"summary_text\":\"\"}", stopReason);
        assertInvalidResponse();
    }

    @Test
    void rejectsMissingTextBlockOrEmptyBody() {
        when(client.summarize("prompt", "Review")).thenReturn(
                "{\"stop_reason\":\"end_turn\",\"content\":[{\"type\":\"thinking\"}]}").thenReturn(null);
        assertInvalidResponse();
        assertInvalidResponse();
    }

    @Test
    void validatesLengthInUnicodeCodePointsAndKoreanSentenceCount() {
        String maxLength = "\uD83D\uDCF7".repeat(150);
        returnsSummary(maxLength);
        assertThat(service.summarize(List.of("Review")).summaryText()).isEqualTo(maxLength);

        returnsSummary(maxLength + "a");
        assertInvalidResponse();

        String threeSentences = "\uC88B\uC558\uC5B4\uC694. ".repeat(2) + "\uC88B\uC558\uC5B4\uC694.";
        returnsSummary(threeSentences);
        assertThat(service.summarize(List.of("Review")).summaryText()).isEqualTo(threeSentences);

        returnsSummary(threeSentences + " \uC88B\uC558\uC5B4\uC694.");
        assertInvalidResponse();

        returnsSummary("\uC88B\uC558\uC5B4\uC694.".repeat(4));
        assertInvalidResponse();
    }

    private void returnsSummary(String summary) {
        returnsOutput(mapper.writeValueAsString(Map.of("status", "ok", "summary_text", summary)), "end_turn");
    }

    private void returnsOutput(String output, String stopReason) {
        when(client.summarize("prompt", "Review")).thenReturn(mapper.writeValueAsString(Map.of(
                "stop_reason", stopReason, "content", List.of(Map.of("type", "text", "text", output)))));
    }

    private void assertInvalidResponse() {
        assertThatThrownBy(() -> service.summarize(List.of("Review")))
                .isInstanceOfSatisfying(ReviewSummaryException.class,
                        exception -> assertThat(exception.kind()).isEqualTo(INVALID_RESPONSE));
    }
}
