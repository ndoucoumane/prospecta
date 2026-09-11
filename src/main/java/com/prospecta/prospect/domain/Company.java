package com.prospecta.prospect.domain;

import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "companies")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company extends TenantAwareEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "website")
    private String website;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "country", nullable = false, length = 10)
    @Builder.Default
    private String country = "SN";

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "external_id", length = 150)
    private String externalId;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_pain_points", columnDefinition = "TEXT")
    private String aiPainPoints;

    @Column(name = "ai_analyzed_at")
    private Instant aiAnalyzedAt;
}
