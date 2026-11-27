package cloud.picon.backend.domain.reservation.entity;

import cloud.picon.backend.domain.proposal.entity.Proposal;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservation", uniqueConstraints =
        @UniqueConstraint(name = "uk_reservation_proposal_id", columnNames = "proposal_id"))
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false,
        updatable = false, columnDefinition = "DATETIME"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false,
        columnDefinition = "DATETIME"))
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservation_proposal"))
    private Proposal proposal;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "reservation_time", nullable = false, columnDefinition = "TIME")
    private LocalTime reservationTime;

    @Column(nullable = false, length = 20)
    @ColumnDefault("'CONFIRMED'")
    private String status = "CONFIRMED";

    @Column(name = "cancel_reason", length = 20)
    private String cancelReason;

    @Column(name = "cancel_detail", columnDefinition = "TEXT")
    private String cancelDetail;

    protected Reservation() {
    }
}
