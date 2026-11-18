package cloud.picon.backend.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import cloud.picon.backend.domain.expert.entity.Expert;
import cloud.picon.backend.global.entity.BaseCreatedEntity;

@Entity
@Table(name = "favorite", uniqueConstraints = @UniqueConstraint(
        name = "uk_favorite_member_expert", columnNames = {"member_id", "expert_member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_expert"))
    private Expert expert;

    public Favorite(Member member, Expert expert) {
        this.member = member;
        this.expert = expert;
    }
}
