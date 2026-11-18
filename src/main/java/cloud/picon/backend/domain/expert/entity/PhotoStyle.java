package cloud.picon.backend.domain.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "photo_style")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_style_id")
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    public PhotoStyle(String name) {
        this.name = name;
    }
}
