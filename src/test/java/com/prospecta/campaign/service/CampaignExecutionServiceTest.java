package com.prospecta.campaign.service;

import com.prospecta.campaign.domain.*;
import com.prospecta.campaign.repository.CampaignProspectRepository;
import com.prospecta.campaign.repository.CampaignStepRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.outbox.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignExecutionServiceTest {

    @Mock
    private CampaignProspectRepository campaignProspectRepository;

    @Mock
    private CampaignStepRepository campaignStepRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CampaignExecutionService campaignExecutionService;

    @Test
    @DisplayName("Should execute due step 1, render message variables, dispatch to outbox, and advance to step 2")
    void shouldExecuteDueStepAndAdvanceTarget() {
        UUID campaignId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID prospectId = UUID.randomUUID();

        Campaign campaign = Campaign.builder()
                .name("Campagne Hôtels")
                .status(CampaignStatus.RUNNING)
                .build();
        campaign.setId(campaignId);
        campaign.setOrganizationId(orgId);

        Prospect prospect = Prospect.builder()
                .firstName("Babacar")
                .lastName("Gueye")
                .fullName("Babacar Gueye")
                .companyName("Hotel Palm Beach")
                .email("babacar@palmbeach.sn")
                .status(ProspectStatus.NEW)
                .build();
        prospect.setId(prospectId);
        prospect.setOrganizationId(orgId);

        CampaignProspect cp = CampaignProspect.builder()
                .campaign(campaign)
                .prospect(prospect)
                .status(CampaignProspectStatus.ACTIVE)
                .currentStep(1)
                .nextActionAt(Instant.now().minusSeconds(60))
                .build();

        CampaignStep step1 = CampaignStep.builder()
                .campaign(campaign)
                .position(1)
                .channel(ChannelType.EMAIL)
                .subjectTemplate("Partenariat {companyName}")
                .contentTemplate("Bonjour {firstName}, découvrez notre solution pour {companyName}.")
                .enabled(true)
                .build();

        CampaignStep step2 = CampaignStep.builder()
                .campaign(campaign)
                .position(2)
                .channel(ChannelType.WHATSAPP)
                .delayMinutes(2880) // 2 days
                .contentTemplate("Bonjour {firstName}, avez-vous pu consulter mon email ?")
                .enabled(true)
                .build();

        when(campaignProspectRepository.findDueProspects(eq(CampaignProspectStatus.ACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(cp));
        when(campaignStepRepository.findByCampaignIdAndPosition(campaignId, 1)).thenReturn(Optional.of(step1));
        when(campaignStepRepository.findByCampaignIdAndPosition(campaignId, 2)).thenReturn(Optional.of(step2));

        int executed = campaignExecutionService.executeDueSteps();

        assertThat(executed).isEqualTo(1);
        assertThat(cp.getCurrentStep()).isEqualTo(2);
        assertThat(cp.getNextActionAt()).isAfter(Instant.now());
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.CONTACTED);

        verify(outboxService).recordEvent(eq("Message"), eq(prospectId.toString()), eq("message.send.requested"), any());
        verify(campaignProspectRepository).save(cp);
        verify(prospectRepository).save(prospect);
    }

    @Test
    @DisplayName("Condition: Should stop sequence if prospect has already replied")
    void shouldStopSequenceIfProspectReplied() {
        UUID campaignId = UUID.randomUUID();
        Campaign campaign = Campaign.builder().status(CampaignStatus.RUNNING).build();
        campaign.setId(campaignId);

        Prospect prospect = Prospect.builder()
                .status(ProspectStatus.REPLIED)
                .build();

        CampaignProspect cp = CampaignProspect.builder()
                .campaign(campaign)
                .prospect(prospect)
                .status(CampaignProspectStatus.ACTIVE)
                .currentStep(2)
                .build();

        when(campaignProspectRepository.findDueProspects(eq(CampaignProspectStatus.ACTIVE), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(cp));

        int executed = campaignExecutionService.executeDueSteps();

        assertThat(executed).isEqualTo(1);
        assertThat(cp.getStatus()).isEqualTo(CampaignProspectStatus.REPLIED);
        assertThat(cp.getCompletedAt()).isNotNull();

        verify(outboxService, never()).recordEvent(any(), any(), any(), any());
        verify(campaignProspectRepository).save(cp);
    }
}
