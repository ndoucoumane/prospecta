package com.prospecta.organization.dto;

import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.domain.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String slug;
    private String country;
    private String timezone;
    private String currency;
    private String industry;
    private String website;
    private String phone;
    private String email;
    private OrganizationStatus status;
    private OrganizationPlan plan;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrganizationResponse from(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .country(org.getCountry())
                .timezone(org.getTimezone())
                .currency(org.getCurrency())
                .industry(org.getIndustry())
                .website(org.getWebsite())
                .phone(org.getPhone())
                .email(org.getEmail())
                .status(org.getStatus())
                .plan(org.getPlan())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
