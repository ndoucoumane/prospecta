package com.prospecta.campaign.domain;

import com.prospecta.prospect.domain.Prospect;
import com.prospecta.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "campaign_prospects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProspect extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private CampaignProspectStatus status = CampaignProspectStatus.PENDING;

    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private int currentStep = 1;

    @Column(name = "next_action_at")
    private Instant nextActionAt;

    @Column(name = "last_action_at")
    private Instant lastActionAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
