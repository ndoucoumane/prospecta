package com.prospecta.pipeline.domain;

import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "opportunities")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Opportunity extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 50)
    @Builder.Default
    private OpportunityStage stage = OpportunityStage.NEW;

    @Column(name = "estimated_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal estimatedValue = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XOF";

    @Column(name = "win_probability", nullable = false)
    @Builder.Default
    private Integer winProbability = 10;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "loss_reason")
    private String lossReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
