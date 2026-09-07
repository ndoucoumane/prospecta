package com.prospecta.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

public final class AiDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRequest {
        private String systemPrompt;
        private String userPrompt;
        private String model;
        @Builder.Default
        private double temperature = 0.2;
        @Builder.Default
        private int maxTokens = 1500;
        @Builder.Default
        private boolean responseFormatJson = true;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiResponse {
        private String content;
        private String model;
        private int inputTokens;
        private int outputTokens;
        private long durationMs;
        private boolean success;
        private String errorMessage;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompanyAnalysisResult {
        private String summary;
        private String industry;
        private List<String> painPoints;
        private List<String> opportunities;
        private String recommendedApproach;
        private double confidence;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProspectSummaryResult {
        private String summary;
        private List<String> keyStrengths;
        private String suggestedAngle;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateMessageRequest {
        @NotBlank(message = "Channel is required (WHATSAPP, EMAIL, SMS)")
        private String channel;

        @NotNull(message = "Prospect ID is required")
        private UUID prospectId;

        @NotBlank(message = "Offer description is required")
        private String offerDescription;

        private String goal;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateMessageResult {
        private String channel;
        private String subject;
        private String body;
        private String callToAction;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiUsageSummary {
        private long monthlyOperations;
        private long monthlyTokens;
        private long quotaLimit;
        private String plan;
    }
}
