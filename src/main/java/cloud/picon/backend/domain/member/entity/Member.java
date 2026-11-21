package cloud.picon.backend.domain.member.entity;

import cloud.picon.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(unique = true)
    private Long kakaoId;

    @Column(length = 20)
    private String nickname;

    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private ServiceType serviceType;

    @Column(length = 200)
    private String introduction;

    private LocalDateTime termsAgreedAt;

    private LocalDateTime marketingAgreedAt;

    private LocalDateTime privacyAgreedAt;

    private LocalDateTime onboardedAt;

    public static Member createKakaoMember(Long kakaoId) {
        Member member = new Member();
        member.kakaoId = kakaoId;
        return member;
    }

    public boolean isOnboarded() {
        return onboardedAt != null;
    }

    // 약관 동의 일시와 온보딩 완료 일시는 같은 시각으로 기록. 알림 수신은 동의한 경우에만 기록
    public void completeOnboarding(String nickname, LocalDate birth, Gender gender, String phoneNumber,
                                   ServiceType serviceType, boolean marketingAgreed, LocalDateTime now) {
        this.nickname = nickname;
        this.birth = birth;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.serviceType = serviceType;
        this.termsAgreedAt = now;
        this.privacyAgreedAt = now;
        this.marketingAgreedAt = marketingAgreed ? now : null;
        this.onboardedAt = now;
    }
}