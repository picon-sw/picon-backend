package cloud.picon.backend.domain.member.entity;

import cloud.picon.backend.domain.mood.Mood;
import cloud.picon.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "member_mood", uniqueConstraints = @UniqueConstraint(
        name = "uk_member_mood_member_mood", columnNames = {"member_id", "mood"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberMood extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_mood_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_mood_member"))
    private Member member;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Mood mood;

    public MemberMood(Member member, Mood mood) {
        this.member = member;
        this.mood = mood;
    }
}