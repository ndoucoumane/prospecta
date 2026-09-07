package com.prospecta.prospect.dto;

import com.prospecta.prospect.domain.IdealCustomerProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

public final class IcpDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateIcpRequest {
        @NotBlank(message = "ICP name is required")
        @Size(max = 255)
        private String name;
        private String description;
        private String targetIndustries;
        private String targetCities;
        private String targetCountries;
        private Integer minEmployees;
        private Integer maxEmployees;
        private String targetJobTitles;
        private String keywords;
        private String excludedIndustries;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateIcpRequest {
        @Size(max = 255)
        private String name;
        private String description;
        private String targetIndustries;
        private String targetCities;
        private String targetCountries;
        private Integer minEmployees;
        private Integer maxEmployees;
        private String targetJobTitles;
        private String keywords;
        private String excludedIndustries;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IcpResponse {
        private UUID id;
        private UUID organizationId;
        private String name;
        private String description;
        private String targetIndustries;
        private String targetCities;
        private String targetCountries;
        private Integer minEmployees;
        private Integer maxEmployees;
        private String targetJobTitles;
        private String keywords;
        private String excludedIndustries;
        private Instant createdAt;
        private Instant updatedAt;

        public static IcpResponse from(IdealCustomerProfile icp) {
            return IcpResponse.builder()
                    .id(icp.getId())
                    .organizationId(icp.getOrganizationId())
                    .name(icp.getName())
                    .description(icp.getDescription())
                    .targetIndustries(icp.getTargetIndustries())
                    .targetCities(icp.getTargetCities())
                    .targetCountries(icp.getTargetCountries())
                    .minEmployees(icp.getMinEmployees())
                    .maxEmployees(icp.getMaxEmployees())
                    .targetJobTitles(icp.getTargetJobTitles())
                    .keywords(icp.getKeywords())
                    .excludedIndustries(icp.getExcludedIndustries())
                    .createdAt(icp.getCreatedAt())
                    .updatedAt(icp.getUpdatedAt())
                    .build();
        }
    }
}
