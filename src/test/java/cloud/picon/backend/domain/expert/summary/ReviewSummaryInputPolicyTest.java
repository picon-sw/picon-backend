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
            "사진 10장, 보정은 2일 걸렸어요.",
            "참고 번호 123456789입니다.",
            "한글"
    })
    void retainsOrdinaryReviewText(String body) {
        assertThat(ReviewSummaryInputPolicy.canSend(body)).isTrue();
    }
}
