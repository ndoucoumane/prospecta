package com.prospecta.campaign.service;

import com.prospecta.campaign.domain.*;
import com.prospecta.campaign.dto.CampaignDto.*;
import com.prospecta.campaign.repository.CampaignProspectRepository;
import com.prospecta.campaign.repository.CampaignRepository;
import com.prospecta.campaign.repository.CampaignStepRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.outbox.OutboxService;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignStepRepository campaignStepRepository;

    @Mock
    private CampaignProspectRepository campaignProspectRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CampaignService campaignService;

    private UUID organizationId;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        campaignId = UUID.randomUUID();

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .organizationId(organizationId)
                .email("admin@prospecta.sn")
                .permissions(Collections.emptySet())
                .build();
        TenantContextHolder.setContext(TenantContext.of(organizationId, principal, "trace-campaign-1"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should create campaign in DRAFT status")
    void shouldCreateCampaign() {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("Hôtels Dakar - Offre Site Web")
                .description("Campagne multicanale pour hôtels à Dakar")
                .channelStrategy("MULTI_CHANNEL")
                .build();

        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            c.setId(campaignId);
            return c;
        });

        CampaignResponse response = campaignService.createCampaign(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Hôtels Dakar - Offre Site Web");
        assertThat(response.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        verify(auditService).logSync(eq("CAMPAIGN_CREATED"), eq("Campaign"), any(), any());
    }

    @Test
    @DisplayName("Should launch campaign, activate targets, and record outbox event")
    void shouldLaunchCampaignSuccessfully() {
        Campaign campaign = Campaign.builder()
                .name("Hôtels Dakar")
                .status(CampaignStatus.READY)
                .build();
        campaign.setId(campaignId);
        campaign.setOrganizationId(organizationId);

        CampaignStep step1 = CampaignStep.builder().position(1).channel(ChannelType.EMAIL).contentTemplate("Bonjour").build();
        campaign.addStep(step1);

        when(campaignRepository.findByIdAndOrganizationIdWithSteps(campaignId, organizationId))
                .thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignProspect cp = CampaignProspect.builder().status(CampaignProspectStatus.PENDING).build();
        when(campaignProspectRepository.findAllByCampaignId(eq(campaignId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cp)));

        CampaignResponse response = campaignService.launchCampaign(campaignId);

        assertThat(response.getStatus()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(cp.getStatus()).isEqualTo(CampaignProspectStatus.ACTIVE);
        assertThat(cp.getNextActionAt()).isNotNull();

        verify(outboxService).recordEvent(eq("Campaign"), eq(campaignId.toString()), eq("campaign.started"), any());
        verify(auditService).logSync(eq("CAMPAIGN_LAUNCHED"), eq("Campaign"), eq(campaignId.toString()), any());
    }
}
