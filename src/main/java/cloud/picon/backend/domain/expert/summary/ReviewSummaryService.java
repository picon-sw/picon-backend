package cloud.picon.backend.domain.expert.summary;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cloud.picon.backend.domain.expert.summary.ReviewSummaryException.Kind.CONFIGURATION;
import static cloud.picon.backend.domain.expert.summary.ReviewSummaryException.Kind.INVALID_RESPONSE;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;

public class ReviewSummaryService {

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");
    private static final Pattern UNSPACED_KOREAN_PERIOD = Pattern.compile("(?<=\\p{IsHangul})\\.(?=\\p{IsHangul})");
    private static final Pattern KOREAN_PERIOD_BEFORE_DIGIT =
            Pattern.compile("(?<=\\p{IsHangul})\\.(?=\\s*\\p{Nd})", Pattern.UNICODE_CHARACTER_CLASS);

    private final ClaudeReviewSummaryClient client;
    private final ClaudeReviewSummaryProperties properties;
    private final JsonMapper jsonMapper;

    ReviewSummaryService(ClaudeReviewSummaryClient client, ClaudeReviewSummaryProperties properties,
                         JsonMapper jsonMapper) {
        this.client = client;
        this.properties = properties;
        this.jsonMapper = jsonMapper.rebuild().enable(FAIL_ON_READING_DUP_TREE_KEY, FAIL_ON_TRAILING_TOKENS).build();
    }

    public ReviewSummaryResult summarize(List<String> reviews) {
        if (reviews == null || reviews.stream().anyMatch(review -> review == null)) {
            throw new IllegalArgumentException("Reviews and review entries must be non-null");
        }
        if (reviews.stream().allMatch(ReviewSummaryService::isBlank)) {
            return new ReviewSummaryResult("fail", "");
        }
        if (reviews.stream().anyMatch(ReviewSummaryService::isBlank)) {
            throw new IllegalArgumentException("Review entries must contain text");
        }
        String content = reviews.stream()
                .map(review -> LINE_BREAK.matcher(review).replaceAll(" "))
                .collect(Collectors.joining("\n"));

        JsonNode message = parseObject(client.summarize(loadPrompt(), content));
        if (!"end_turn".equals(textField(message, "stop_reason")) || !message.path("content").isArray()) {
            throw invalidResponse();
        }
        StringBuilder output = new StringBuilder();
        for (JsonNode block : message.path("content")) {
            if ("text".equals(textField(block, "type"))) {
                String text = textField(block, "text");
                if (text == null) {
                    throw invalidResponse();
                }
                output.append(text);
            }
        }
        return validateResult(parseObject(output.toString()));
    }

    private String loadPrompt() {
        if (properties.prompt() != null) {
            try {
                String prompt = properties.prompt().getContentAsString(StandardCharsets.UTF_8);
                if (!isBlank(prompt)) {
                    return prompt;
                }
            } catch (IOException e) {
                throw new ReviewSummaryException(CONFIGURATION, "Review summary prompt could not be read");
            }
        }
        throw new ReviewSummaryException(CONFIGURATION, "A review summary prompt is required");
    }

    private JsonNode parseObject(String json) {
        if (json != null) {
            try {
                JsonNode parsed = jsonMapper.readTree(json);
                if (parsed != null && parsed.isObject()) {
                    return parsed;
                }
            } catch (JacksonException e) {
                throw invalidResponse();
            }
        }
        throw invalidResponse();
    }

    private ReviewSummaryResult validateResult(JsonNode result) {
        String status = textField(result, "status");
        String summary = textField(result, "summary_text");
        if (result.size() != 2 || status == null || summary == null) {
            throw invalidResponse();
        }
        switch (status) {
            case "fail" -> {
                if (!summary.isEmpty()) {
                    throw invalidResponse();
                }
            }
            case "ok" -> {
                if (isBlank(summary) || summary.codePointCount(0, summary.length()) > 150
                        || sentenceCount(summary) > 3) {
                    throw invalidResponse();
                }
            }
            default -> throw invalidResponse();
        }
        return new ReviewSummaryResult(status, summary);
    }

    private static int sentenceCount(String summary) {
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.KOREAN);
        String normalized = UNSPACED_KOREAN_PERIOD.matcher(summary).replaceAll(". ");
        // BreakIterator can skip an ASCII period before a digit. A full stop marks this boundary.
        sentences.setText(KOREAN_PERIOD_BEFORE_DIGIT.matcher(normalized).replaceAll("\u3002"));
        int count = 0;
        for (int end = sentences.next(); end != BreakIterator.DONE; end = sentences.next()) {
            count++;
        }
        return count;
    }

    private static boolean isBlank(String value) {
        return value.codePoints().allMatch(cp ->
                Character.isWhitespace(cp) || Character.isSpaceChar(cp) || cp == 0x85);
    }

    private static String textField(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.stringValue() : null;
    }

    private static ReviewSummaryException invalidResponse() {
        return new ReviewSummaryException(INVALID_RESPONSE, "Claude returned an invalid review summary response");
    }
}
