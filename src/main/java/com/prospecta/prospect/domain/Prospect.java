package com.prospecta.prospect.domain;

import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "prospects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prospect extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "email")
    private String email;

    @Column(name = "email_status", nullable = false, length = 50)
    @Builder.Default
    private String emailStatus = "UNKNOWN";

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "phone_status", nullable = false, length = 50)
    @Builder.Default
    private String phoneStatus = "UNKNOWN";

    @Column(name = "whatsapp_number", length = 50)
    private String whatsappNumber;

    @Column(name = "country", nullable = false, length = 10)
    @Builder.Default
    private String country = "SN";

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "company_size", length = 50)
    private String companySize;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "source", nullable = false, length = 100)
    @Builder.Default
    private String source = "MANUAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ProspectStatus status = ProspectStatus.NEW;

    @Column(name = "lead_score", nullable = false)
    @Builder.Default
    private int leadScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_score_level", nullable = false, length = 50)
    @Builder.Default
    private LeadScoreLevel leadScoreLevel = LeadScoreLevel.LOW;

    @Column(name = "lead_score_reasons", columnDefinition = "TEXT")
    private String leadScoreReasons;

    public UUID getCompanyId() {
        return company != null ? company.getId() : null;
    }

    public String computeFullName() {
        if (firstName == null && lastName == null) {
            return email != null ? email : "Inconnu";
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}
