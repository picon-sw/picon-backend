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
                && body.codePoints().anyMatch(cp -> !isReviewSpace(cp))
                && !containsPhoneLikeNumber(body)
                && !EMAIL_OR_URL.matcher(body).find();
    }

    private static boolean containsPhoneLikeNumber(String body) {
        int digits = 0;
        for (int i = 0; i < body.length();) {
            int codePoint = body.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isDigit(codePoint)) {
                if (++digits == 10) {
                    return true;
                }
            } else if (!isPhoneSeparator(codePoint)) {
                digits = 0;
            }
        }
        return false;
    }

    private static boolean isPhoneSeparator(int codePoint) {
        return isReviewSpace(codePoint) || switch (codePoint) {
            case '(', ')', '.', '+', '-', '/', '\u00b7', '\u2013' -> true;
            default -> false;
        };
    }

    private static boolean isReviewSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || codePoint == 0x85;
    }
}
