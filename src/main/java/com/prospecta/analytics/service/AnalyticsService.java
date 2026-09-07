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
import com.prospecta.shared.exception.CampaignNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProspectRepository prospectRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignProspectRepository campaignProspectRepository;
    private final MessageRepository messageRepository;
    private final OpportunityRepository opportunityRepository;

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverview() {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        long totalProspects = prospectRepository.countByOrganizationId(organizationId);
        long qualifiedProspects = prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.QUALIFIED);
        long contactedProspects = prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.CONTACTED);
        long repliedProspects = prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.REPLIED);
        long meetingBookedProspects = prospectRepository.countByOrganizationIdAndStatus(organizationId, ProspectStatus.MEETING_BOOKED);

        long opportunitiesCount = opportunityRepository.countByOrganizationId(organizationId);
        long opportunitiesWon = opportunityRepository.countByOrganizationIdAndStage(organizationId, OpportunityStage.WON);
        BigDecimal wonValue = opportunityRepository.sumEstimatedValueByStage(organizationId, OpportunityStage.WON);
        BigDecimal pipelineValue = opportunityRepository.sumTotalEstimatedValue(organizationId);

        long activeCampaigns = campaignRepository.countByOrganizationIdAndStatus(organizationId, CampaignStatus.RUNNING);

        long engagedBase = contactedProspects + repliedProspects + meetingBookedProspects;
        double replyRate = engagedBase > 0
                ? BigDecimal.valueOf((double) (repliedProspects + meetingBookedProspects) / engagedBase * 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        double conversionRate = totalProspects > 0
                ? BigDecimal.valueOf((double) opportunitiesWon / totalProspects * 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        List<ChannelMetricDto> channelBreakdown = new ArrayList<>();
        for (ChannelType channel : List.of(ChannelType.WHATSAPP, ChannelType.EMAIL, ChannelType.SMS)) {
            long sent = messageRepository.countByOrganizationIdAndChannel(organizationId, channel);
            long delivered = messageRepository.countByOrganizationIdAndChannelAndStatus(organizationId, channel, MessageStatus.DELIVERED);
            long failed = messageRepository.countByOrganizationIdAndChannelAndStatus(organizationId, channel, MessageStatus.FAILED);
            channelBreakdown.add(ChannelMetricDto.builder()
                    .channel(channel)
                    .totalSent(sent)
                    .delivered(delivered)
                    .failed(failed)
                    .build());
        }

        return AnalyticsOverviewResponse.builder()
                .totalProspects(totalProspects)
                .qualifiedProspects(qualifiedProspects)
                .contactedProspects(contactedProspects)
                .repliedProspects(repliedProspects)
                .meetingBookedProspects(meetingBookedProspects)
                .opportunitiesCount(opportunitiesCount)
                .opportunitiesWon(opportunitiesWon)
                .totalWonValue(wonValue != null ? wonValue : BigDecimal.ZERO)
                .totalPipelineValue(pipelineValue != null ? pipelineValue : BigDecimal.ZERO)
                .currency("XOF")
                .replyRate(replyRate)
                .conversionRate(conversionRate)
                .activeCampaigns(activeCampaigns)
                .channelBreakdown(channelBreakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public CampaignAnalyticsResponse getCampaignAnalytics(UUID campaignId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        Campaign campaign = campaignRepository.findByIdAndOrganizationId(campaignId, organizationId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        long targetCount = campaignProspectRepository.countByCampaignId(campaignId);
        long pending = campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.PENDING);
        long active = campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.ACTIVE);
        long completed = campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.COMPLETED);
        long replied = campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.REPLIED);
        long optedOut = campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.OPTED_OUT);
        long failed = campaignProspectRepository.countByCampaignIdAndStatus(campaignId, CampaignProspectStatus.FAILED);

        double replyRate = targetCount > 0
                ? BigDecimal.valueOf((double) replied / targetCount * 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return CampaignAnalyticsResponse.builder()
                .campaignId(campaign.getId())
                .campaignName(campaign.getName())
                .status(campaign.getStatus())
                .targetProspectsCount(targetCount)
                .pendingCount(pending)
                .activeCount(active)
                .completedCount(completed)
                .repliedCount(replied)
                .optedOutCount(optedOut)
                .failedCount(failed)
                .replyRate(replyRate)
                .build();
    }
}
