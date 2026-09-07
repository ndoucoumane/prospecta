package com.prospecta.analytics.service;

import com.prospecta.analytics.dto.AnalyticsDto.*;
import com.prospecta.campaign.domain.Campaign;
import com.prospecta.campaign.domain.CampaignProspectStatus;
import com.prospecta.campaign.domain.CampaignStatus;
import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.campaign.repository.CampaignProspectRepository;
import com.prospecta.campaign.repository.CampaignRepository;
import com.prospecta.messaging.domain.MessageStatus;
import com.prospecta.messaging.repository.MessageRepository;
import com.prospecta.pipeline.domain.OpportunityStage;
import com.prospecta.pipeline.repository.OpportunityRepository;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignProspectRepository campaignProspectRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID organizationId;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        campaignId = UUID.randomUUID();

        TenantContextHolder.setContext(TenantContext.builder()
                .organizationId(organizationId)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Get overview calculates reply rates, conversion rates, and channel statistics")
    void getOverview_calculatesRatesAndMetrics() {
        when(prospectRepository.countByOrganizationId(organizationId)).thenReturn(100L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.QUALIFIED)).thenReturn(40L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.CONTACTED)).thenReturn(50L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.REPLIED)).thenReturn(15L);
        when(prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.MEETING_BOOKED)).thenReturn(5L);

        when(opportunityRepository.countByOrganizationId(organizationId)).thenReturn(10L);
        when(opportunityRepository.countByOrganizationIdAndStage(organizationId, OpportunityStage.WON)).thenReturn(4L);
        when(opportunityRepository.sumEstimatedValueByStage(organizationId, OpportunityStage.WON)).thenReturn(BigDecimal.valueOf(12_000_000));
        when(opportunityRepository.sumTotalEstimatedValue(organizationId)).thenReturn(BigDecimal.valueOf(30_000_000));

        when(campaignRepository.countByOrganizationIdAndStatus(organizationId, CampaignStatus.RUNNING)).thenReturn(2L);

        when(messageRepository.countByOrganizationIdAndChannel(eq(organizationId), any(ChannelType.class))).thenReturn(25L);
        when(messageRepository.countByOrganizationIdAndChannelAndStatus(eq(organizationId), any(ChannelType.class), eq(MessageStatus.DELIVERED))).thenReturn(23L);
        when(messageRepository.countByOrganizationIdAndChannelAndStatus(eq(organizationId), any(ChannelType.class), eq(MessageStatus.FAILED))).thenReturn(2L);

        AnalyticsOverviewResponse overview = analyticsService.getOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.getTotalProspects()).isEqualTo(100L);
        assertThat(overview.getQualifiedProspects()).isEqualTo(40L);
        assertThat(overview.getContactedProspects()).isEqualTo(50L);
        assertThat(overview.getRepliedProspects()).isEqualTo(15L);
        assertThat(overview.getMeetingBookedProspects()).isEqualTo(5L);
        assertThat(overview.getOpportunitiesWon()).isEqualTo(4L);
        assertThat(overview.getTotalWonValue()).isEqualByComparingTo(BigDecimal.valueOf(12_000_000));
        assertThat(overview.getTotalPipelineValue()).isEqualByComparingTo(BigDecimal.valueOf(30_000_000));
        assertThat(overview.getCurrency()).isEqualTo("XOF");

        // Engaged = 50 + 15 + 5 = 70. Replied/Meeting = 20. 20 / 70 * 100 = 28.57%
        assertThat(overview.getReplyRate()).isEqualTo(28.57);

        // Conversion = 4 / 100 * 100 = 4.0%
        assertThat(overview.getConversionRate()).isEqualTo(4.0);

        assertThat(overview.getActiveCampaigns()).isEqualTo(2L);
        assertThat(overview.getChannelBreakdown()).hasSize(3);
    }

    @Test
    @DisplayName("Get campaign analytics calculates conversion funnel and reply rate")
    void getCampaignAnalytics_calculatesFunnelAndReplyRate() {
        Campaign campaign = Campaign.builder()
                .name("Campagne Directeurs Dakar")
                .status(CampaignStatus.RUNNING)
                .build();
        campaign.setId(campaignId);
        campaign.setOrganizationId(organizationId);

        when(campaignRepository.findByIdAndOrganizationId(campaignId, organizationId))
                .thenReturn(Optional.of(campaign));

        when(campaignProspectRepository.countByCampaignId(campaignId)).thenReturn(50L);
        when(campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.PENDING)).thenReturn(10L);
        when(campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.ACTIVE)).thenReturn(20L);
        when(campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.COMPLETED)).thenReturn(8L);
        when(campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.REPLIED)).thenReturn(10L);
        when(campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.OPTED_OUT)).thenReturn(1L);
        when(campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.FAILED)).thenReturn(1L);

        CampaignAnalyticsResponse response = analyticsService.getCampaignAnalytics(campaignId);

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(campaignId);
        assertThat(response.getCampaignName()).isEqualTo("Campagne Directeurs Dakar");
        assertThat(response.getTargetProspectsCount()).isEqualTo(50L);
        assertThat(response.getRepliedCount()).isEqualTo(10L);
        // Reply rate = 10 / 50 * 100 = 20.0%
        assertThat(response.getReplyRate()).isEqualTo(20.0);
    }
}
