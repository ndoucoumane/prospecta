package com.prospecta.analytics.dto;

import com.prospecta.campaign.domain.CampaignStatus;
import com.prospecta.campaign.domain.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelMetricDto {
        private ChannelType channel;
        private long totalSent;
        private long delivered;
        private long failed;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsOverviewResponse {
        private long totalProspects;
        private long qualifiedProspects;
        private long contactedProspects;
        private long repliedProspects;
        private long meetingBookedProspects;
        private long opportunitiesCount;
        private long opportunitiesWon;
        private BigDecimal totalWonValue;
        private BigDecimal totalPipelineValue;
        private String currency;
        private double replyRate;
        private double conversionRate;
        private long activeCampaigns;
        private List<ChannelMetricDto> channelBreakdown;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignAnalyticsResponse {
        private UUID campaignId;
        private String campaignName;
        private CampaignStatus status;
        private long targetProspectsCount;
        private long pendingCount;
        private long activeCount;
        private long completedCount;
        private long repliedCount;
        private long optedOutCount;
        private long failedCount;
        private double replyRate;
    }
}
