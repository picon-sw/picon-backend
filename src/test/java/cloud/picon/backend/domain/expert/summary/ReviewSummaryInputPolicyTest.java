package cloud.picon.backend.domain.expert.summary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSummaryInputPolicyTest {

    @Test
    void excludesTwoThousandConsecutiveDigitsWithoutRecursion() {
        assertThat(ReviewSummaryInputPolicy.canSend("7".repeat(2_000))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "연락처는 (010) 1234.5678입니다.",
            "전화 +82 (10) 1234-5678",
            "계정은 SOME.ONE+test@example.org입니다",
            "연락은 촬영팀@예시.한국",
            "사진은 HTTPS://example.org/photos 에 있습니다."
    })
    void stillExcludesRecognizableContactDetails(String body) {
        assertThat(ReviewSummaryInputPolicy.canSend(body)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "010 1234 5678",
            "연락처는 010\u00a01234\u00a05678입니다.",
            "010/1234/5678",
            "010·1234·5678",
            "010–1234–5678",
            "０１０-１２３４-５６７８",
            "010\u20091234\u20095678",
            "٠١٠-١٢٣٤-٥٦٧٨",
            "𝟎𝟏𝟎-𝟏𝟐𝟑𝟒-𝟓𝟔𝟕𝟖",
            "01012345678"
    })
    void excludesPhoneLikeDigitRunsAcrossUnicodeDigitsAndSeparators(String body) {
        assertThat(ReviewSummaryInputPolicy.canSend(body)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "사진 10장, 보정은 2일 걸렸어요.",
            "참고 번호 123456789입니다.",
            "촬영 전\u00a0안내도\u2009친절했어요.",
            "사진은 6/12에 받고 원본은 ３일 뒤 받았어요.",
            "참고 번호는 12·34–56789입니다.",
            "한글"
    })
    void retainsOrdinaryReviewText(String body) {
        assertThat(ReviewSummaryInputPolicy.canSend(body)).isTrue();
    }
}
