package cloud.picon.backend.domain.proposal.entity;

import cloud.picon.backend.domain.expert.entity.Expert;
import cloud.picon.backend.domain.request.entity.Request;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "proposal", uniqueConstraints =
        @UniqueConstraint(name = "uk_proposal_request_member", columnNames = {"request_id", "member_id"}))
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false,
        updatable = false, columnDefinition = "DATETIME"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false,
        columnDefinition = "DATETIME"))
public class Proposal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proposal_request"))
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proposal_expert"))
    private Expert expert;

    @Column(nullable = false)
    private int price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "available_time")
    private String availableTime;

    protected Proposal() {
    }
}
