package cloud.picon.backend.domain.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @Column(name = "region_code")
    private Long regionCode;

    @Column(name = "first_depth", nullable = false, length = 20)
    private String firstDepth;

    @Column(name = "second_depth", length = 20)
    private String secondDepth;

    @Column(name = "third_depth", length = 20)
    private String thirdDepth;

    public Region(Long regionCode, String firstDepth, String secondDepth, String thirdDepth) {
        this.regionCode = regionCode;
        this.firstDepth = firstDepth;
        this.secondDepth = secondDepth;
        this.thirdDepth = thirdDepth;
    }
}
