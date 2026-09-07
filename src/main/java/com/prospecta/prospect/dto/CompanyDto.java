package com.prospecta.prospect.dto;

import com.prospecta.prospect.domain.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

public final class CompanyDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCompanyRequest {
        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        private String name;
        private String website;
        private String industry;
        private String description;
        private String country;
        private String city;
        private String phone;
        private String email;
        private Integer employeeCount;
        private String linkedinUrl;
        private String source;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCompanyRequest {
        @Size(max = 255)
        private String name;
        private String website;
        private String industry;
        private String description;
        private String city;
        private String phone;
        private String email;
        private Integer employeeCount;
        private String linkedinUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyResponse {
        private UUID id;
        private UUID organizationId;
        private String name;
        private String website;
        private String industry;
        private String description;
        private String country;
        private String city;
        private String phone;
        private String email;
        private Integer employeeCount;
        private String linkedinUrl;
        private String source;
        private String aiSummary;
        private String aiPainPoints;
        private Instant aiAnalyzedAt;
        private Instant createdAt;
        private Instant updatedAt;

        public static CompanyResponse from(Company company) {
            return CompanyResponse.builder()
                    .id(company.getId())
                    .organizationId(company.getOrganizationId())
                    .name(company.getName())
                    .website(company.getWebsite())
                    .industry(company.getIndustry())
                    .description(company.getDescription())
                    .country(company.getCountry())
                    .city(company.getCity())
                    .phone(company.getPhone())
                    .email(company.getEmail())
                    .employeeCount(company.getEmployeeCount())
                    .linkedinUrl(company.getLinkedinUrl())
                    .source(company.getSource())
                    .aiSummary(company.getAiSummary())
                    .aiPainPoints(company.getAiPainPoints())
                    .aiAnalyzedAt(company.getAiAnalyzedAt())
                    .createdAt(company.getCreatedAt())
                    .updatedAt(company.getUpdatedAt())
                    .build();
        }
    }
}
