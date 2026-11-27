package cloud.picon.backend.domain.review.entity;

import cloud.picon.backend.domain.reservation.entity.Reservation;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_reservation_id", columnNames = "reservation_id"),
        check = @CheckConstraint(name = "ck_review_rating", constraint = "rating BETWEEN 1 AND 5"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_reservation"))
    private Reservation reservation;

    @Column(name = "rating", nullable = false)
    private byte rating;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_tags")
    private String reviewTags;

    @Column(name = "review_content", columnDefinition = "TEXT")
    private String reviewContent;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    protected Review() {
    }
}
