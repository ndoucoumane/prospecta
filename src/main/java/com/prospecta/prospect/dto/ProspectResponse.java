package com.prospecta.prospect.dto;

import com.prospecta.prospect.domain.LeadScoreLevel;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
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
public class ProspectResponse {

    private UUID id;
    private UUID organizationId;
    private UUID companyId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String jobTitle;
    private String companyName;
    private String companyWebsite;
    private String email;
    private String emailStatus;
    private String phone;
    private String phoneStatus;
    private String whatsappNumber;
    private String country;
    private String city;
    private String region;
    private String industry;
    private String companySize;
    private String linkedinUrl;
    private String source;
    private ProspectStatus status;
    private int leadScore;
    private LeadScoreLevel leadScoreLevel;
    private String leadScoreReasons;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProspectResponse from(Prospect prospect) {
        return ProspectResponse.builder()
                .id(prospect.getId())
                .organizationId(prospect.getOrganizationId())
                .companyId(prospect.getCompanyId())
                .firstName(prospect.getFirstName())
                .lastName(prospect.getLastName())
                .fullName(prospect.getFullName())
                .jobTitle(prospect.getJobTitle())
                .companyName(prospect.getCompanyName())
                .companyWebsite(prospect.getCompanyWebsite())
                .email(prospect.getEmail())
                .emailStatus(prospect.getEmailStatus())
                .phone(prospect.getPhone())
                .phoneStatus(prospect.getPhoneStatus())
                .whatsappNumber(prospect.getWhatsappNumber())
                .country(prospect.getCountry())
                .city(prospect.getCity())
                .region(prospect.getRegion())
                .industry(prospect.getIndustry())
                .companySize(prospect.getCompanySize())
                .linkedinUrl(prospect.getLinkedinUrl())
                .source(prospect.getSource())
                .status(prospect.getStatus())
                .leadScore(prospect.getLeadScore())
                .leadScoreLevel(prospect.getLeadScoreLevel())
                .leadScoreReasons(prospect.getLeadScoreReasons())
                .createdAt(prospect.getCreatedAt())
                .updatedAt(prospect.getUpdatedAt())
                .build();
    }
}
