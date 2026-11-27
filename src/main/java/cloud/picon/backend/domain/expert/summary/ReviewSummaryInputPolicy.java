package cloud.picon.backend.domain.expert.summary;

import java.util.regex.Pattern;

final class ReviewSummaryInputPolicy {

    // This heuristic only screens recognizable contact details.
    private static final Pattern EMAIL_OR_URL = Pattern.compile(
            "(?iu)[\\p{L}\\p{N}._%+\\-]+@[\\p{L}\\p{N}.\\-]+\\.[\\p{L}]{2,}|https?://\\S+");

    private ReviewSummaryInputPolicy() {
    }

    static boolean canSend(String body) {
        return body != null
                && body.codePoints().anyMatch(cp -> !Character.isWhitespace(cp)
                        && !Character.isSpaceChar(cp) && cp != 0x85)
                && !containsPhoneLikeNumber(body)
                && !EMAIL_OR_URL.matcher(body).find();
    }

    private static boolean containsPhoneLikeNumber(String body) {
        int digits = 0;
        for (int i = 0; i < body.length(); i++) {
            char character = body.charAt(i);
            if (character >= '0' && character <= '9') {
                if (++digits == 10) {
                    return true;
                }
            } else if (!isPhoneSeparator(character)) {
                digits = 0;
            }
        }
        return false;
    }

    private static boolean isPhoneSeparator(char character) {
        return switch (character) {
            case ' ', '\t', '\n', '\r', '\f', '\u000b', '(', ')', '.', '+', '-' -> true;
            default -> false;
        };
    }
}
