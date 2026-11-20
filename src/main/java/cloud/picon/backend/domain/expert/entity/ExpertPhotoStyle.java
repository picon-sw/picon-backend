package cloud.picon.backend.domain.expert.entity;

import cloud.picon.backend.domain.photostyle.entity.PhotoStyle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import cloud.picon.backend.global.entity.BaseCreatedEntity;

@Entity
@Table(name = "expert_photo_style", uniqueConstraints = @UniqueConstraint(
        name = "uk_expert_photo_style_member_photo_style_id", columnNames = {"member_id", "photo_style_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertPhotoStyle extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_photo_style_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_expert_photo_style_expert"))
    private Expert expert;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_style_id", nullable = false, foreignKey = @ForeignKey(name = "fk_expert_photo_style_style"))
    private PhotoStyle photoStyle;

    public ExpertPhotoStyle(Expert expert, PhotoStyle photoStyle) {
        this.expert = expert;
        this.photoStyle = photoStyle;
    }
}
