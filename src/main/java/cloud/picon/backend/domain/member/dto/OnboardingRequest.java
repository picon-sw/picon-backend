package cloud.picon.backend.domain.member.dto;

import cloud.picon.backend.domain.member.entity.Gender;
import cloud.picon.backend.domain.member.entity.ServiceType;
import cloud.picon.backend.domain.mood.Mood;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public record OnboardingRequest(
        @NotNull(message = "서비스 이용약관 동의 여부를 보내 주세요.")
        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        Boolean termsAgreed,

        @NotNull(message = "개인정보 수집·이용 동의 여부를 보내 주세요.")
        @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다.")
        Boolean privacyAgreed,

        @NotNull(message = "이벤트 및 혜택 알림 수신 동의 여부를 보내 주세요.")
        Boolean marketingAgreed,

        @NotNull(message = "필요한 도움을 선택해 주세요.")
        ServiceType serviceType,

        @NotNull(message = "좋아하는 사진 분위기를 선택해 주세요.")
        @Size(min = 1, max = 3, message = "사진 분위기는 1~3개 선택해 주세요.")
        List<@NotNull(message = "사진 분위기 값이 비어 있습니다.") Mood> moods,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 20, message = "닉네임은 20자 이하로 입력해 주세요.")
        String nickname,

        @Past(message = "생년월일은 오늘 이전 날짜여야 합니다.")
        LocalDate birth,

        Gender gender,

        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 하이픈 없이 숫자 10~11자리로 입력해 주세요.")
        String phoneNumber
) {
    // 같은 분위기를 두 번 보내면 400. result에는 "moodsUnique" 키로 표시됨
    @AssertTrue(message = "사진 분위기는 중복 없이 선택해 주세요.")
    public boolean isMoodsUnique() {
        return moods == null || new HashSet<>(moods).size() == moods.size();
    }
}