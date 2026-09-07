package com.prospecta.campaign.dto;

import com.prospecta.campaign.domain.Campaign;
import com.prospecta.campaign.domain.CampaignStatus;
import com.prospecta.campaign.domain.CampaignStep;
import com.prospecta.campaign.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CampaignDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCampaignRequest {
        @NotBlank(message = "Campaign name is required")
        @Size(max = 255)
        private String name;
        private String description;
        private String channelStrategy;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCampaignRequest {
        @Size(max = 255)
        private String name;
        private String description;
        private String channelStrategy;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignStepRequest {
        private int position;

        @NotNull(message = "Channel is required")
        private ChannelType channel;

        private int delayMinutes;
        private String subjectTemplate;

        @NotBlank(message = "Content template is required")
        private String contentTemplate;

        private String conditions;
        @Builder.Default
        private boolean enabled = true;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignStepResponse {
        private UUID id;
        private int position;
        private ChannelType channel;
        private int delayMinutes;
        private String subjectTemplate;
        private String contentTemplate;
        private boolean enabled;

        public static CampaignStepResponse from(CampaignStep step) {
            return CampaignStepResponse.builder()
                    .id(step.getId())
                    .position(step.getPosition())
                    .channel(step.getChannel())
                    .delayMinutes(step.getDelayMinutes())
                    .subjectTemplate(step.getSubjectTemplate())
                    .contentTemplate(step.getContentTemplate())
                    .enabled(step.isEnabled())
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddProspectsRequest {
        @NotEmpty(message = "Prospect IDs list cannot be empty")
        private List<UUID> prospectIds;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignResponse {
        private UUID id;
        private UUID organizationId;
        private String name;
        private String description;
        private CampaignStatus status;
        private String channelStrategy;
        private Instant startedAt;
        private Instant completedAt;
        private List<CampaignStepResponse> steps;
        private Instant createdAt;
        private Instant updatedAt;

        public static CampaignResponse from(Campaign campaign) {
            return CampaignResponse.builder()
                    .id(campaign.getId())
                    .organizationId(campaign.getOrganizationId())
                    .name(campaign.getName())
                    .description(campaign.getDescription())
                    .status(campaign.getStatus())
                    .channelStrategy(campaign.getChannelStrategy())
                    .startedAt(campaign.getStartedAt())
                    .completedAt(campaign.getCompletedAt())
                    .steps(campaign.getSteps() != null ? campaign.getSteps().stream().map(CampaignStepResponse::from).toList() : List.of())
                    .createdAt(campaign.getCreatedAt())
                    .updatedAt(campaign.getUpdatedAt())
                    .build();
        }
    }
}
