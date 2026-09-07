package com.prospecta.messaging.domain;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "communication_consents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationConsent extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ConsentStatus status = ConsentStatus.UNKNOWN;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "obtained_at")
    private Instant obtainedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public UUID getProspectId() {
        return prospect != null ? prospect.getId() : null;
    }
}
