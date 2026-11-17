package cloud.picon.backend.domain.expert.entity;

import cloud.picon.backend.domain.member.entity.Member;
import cloud.picon.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expert extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "career_years", nullable = false)
    @ColumnDefault("0")
    private short careerYears = 0;

    @Column(name = "avg_rating", nullable = false, precision = 2, scale = 1)
    @ColumnDefault("0.0")
    private BigDecimal avgRating = new BigDecimal("0.0");

    @Column(name = "review_count", nullable = false)
    @ColumnDefault("0")
    private int reviewCount = 0;

    @Column(name = "review_summary", columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(name = "review_summary_count", nullable = false)
    @ColumnDefault("0")
    private int reviewSummaryCount = 0;

    @Column(name = "review_summary_updated_at")
    private LocalDateTime reviewSummaryUpdatedAt;
}
