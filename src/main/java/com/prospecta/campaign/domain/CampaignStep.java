package com.prospecta.campaign.domain;

import com.prospecta.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "campaign_steps")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "position", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Column(name = "delay_minutes", nullable = false)
    @Builder.Default
    private int delayMinutes = 0;

    @Column(name = "subject_template")
    private String subjectTemplate;

    @Column(name = "content_template", nullable = false, columnDefinition = "TEXT")
    private String contentTemplate;

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
