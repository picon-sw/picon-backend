package cloud.picon.backend.domain.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import cloud.picon.backend.global.entity.BaseCreatedEntity;

@Entity
@Table(name = "expert_region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertRegion extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_region_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_expert_region_expert"))
    private Expert expert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_code", nullable = false, foreignKey = @ForeignKey(name = "fk_expert_region_region"))
    private Region region;

    public ExpertRegion(Expert expert, Region region) {
        this.expert = expert;
        this.region = region;
    }
}
