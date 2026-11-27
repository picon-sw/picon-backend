package cloud.picon.backend.domain.request.entity;

import cloud.picon.backend.domain.member.entity.Member;
import cloud.picon.backend.domain.photostyle.entity.PhotoStyle;
import cloud.picon.backend.domain.region.entity.Region;
import cloud.picon.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Table(name = "`request`")
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false,
        updatable = false, columnDefinition = "DATETIME"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false,
        columnDefinition = "DATETIME"))
public class Request extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_request_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code", foreignKey = @ForeignKey(name = "fk_request_region"))
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_style_id", foreignKey = @ForeignKey(name = "fk_request_photo_style"))
    private PhotoStyle photoStyle;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "people_count")
    private Short peopleCount;

    private Integer budget;

    @Column(columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "edit_intensity", length = 10)
    private String editIntensity;

    @Column(nullable = false, length = 20)
    @ColumnDefault("'REQUESTED'")
    private String status = "REQUESTED";

    protected Request() {
    }
}
