package com.prospecta.pipeline.dto;

import com.prospecta.pipeline.domain.Opportunity;
import com.prospecta.pipeline.domain.OpportunityStage;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OpportunityDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOpportunityRequest {
        @NotNull(message = "Prospect ID is required")
        private UUID prospectId;

        private UUID companyId;
        private UUID assignedTo;

        @NotBlank(message = "Title is required")
        private String title;

        private OpportunityStage stage;

        @PositiveOrZero(message = "Estimated value must be positive or zero")
        private BigDecimal estimatedValue;

        private String currency;

        @Min(value = 0, message = "Win probability must be between 0 and 100")
        @Max(value = 100, message = "Win probability must be between 0 and 100")
        private Integer winProbability;

        private LocalDate expectedCloseDate;
        private String notes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateOpportunityRequest {
        private String title;
        private UUID assignedTo;
        private BigDecimal estimatedValue;
        private String currency;
        private Integer winProbability;
        private LocalDate expectedCloseDate;
        private String notes;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateOpportunityStageRequest {
        @NotNull(message = "Stage is required")
        private OpportunityStage stage;

        private String lossReason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpportunityResponse {
        private UUID id;
        private UUID prospectId;
        private String prospectName;
        private UUID companyId;
        private String companyName;
        private UUID assignedTo;
        private String title;
        private OpportunityStage stage;
        private BigDecimal estimatedValue;
        private String currency;
        private Integer winProbability;
        private LocalDate expectedCloseDate;
        private Instant closedAt;
        private String lossReason;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;

        public static OpportunityResponse from(Opportunity opp) {
            return OpportunityResponse.builder()
                    .id(opp.getId())
                    .prospectId(opp.getProspect() != null ? opp.getProspect().getId() : null)
                    .prospectName(opp.getProspect() != null ? opp.getProspect().getFullName() : null)
                    .companyId(opp.getCompany() != null ? opp.getCompany().getId() : null)
                    .companyName(opp.getCompany() != null ? opp.getCompany().getName() : (opp.getProspect() != null ? opp.getProspect().getCompanyName() : null))
                    .assignedTo(opp.getAssignedTo())
                    .title(opp.getTitle())
                    .stage(opp.getStage())
                    .estimatedValue(opp.getEstimatedValue())
                    .currency(opp.getCurrency())
                    .winProbability(opp.getWinProbability())
                    .expectedCloseDate(opp.getExpectedCloseDate())
                    .closedAt(opp.getClosedAt())
                    .lossReason(opp.getLossReason())
                    .notes(opp.getNotes())
                    .createdAt(opp.getCreatedAt())
                    .updatedAt(opp.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineStageSummary {
        private OpportunityStage stage;
        private long count;
        private BigDecimal totalValue;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineOverviewResponse {
        private long totalOpportunities;
        private BigDecimal totalPipelineValue;
        private String currency;
        private List<PipelineStageSummary> stages;
    }
}
