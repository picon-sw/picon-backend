package cloud.picon.backend.domain.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import cloud.picon.backend.global.entity.BaseTimeEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "expert_specialty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertSpecialty extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_specialty_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_expert_specialty_expert"))
    private Expert expert;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Specialty name;

    @Column(name = "base_price", nullable = false)
    private int basePrice;

    public ExpertSpecialty(Expert expert, Specialty name, int basePrice) {
        this.expert = expert;
        this.name = name;
        this.basePrice = basePrice;
    }
}
