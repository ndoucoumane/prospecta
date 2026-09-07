package com.prospecta.prospect.domain;

import com.prospecta.shared.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "ideal_customer_profiles")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdealCustomerProfile extends TenantAwareEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_industries", columnDefinition = "TEXT")
    private String targetIndustries;

    @Column(name = "target_cities", columnDefinition = "TEXT")
    private String targetCities;

    @Column(name = "target_countries", columnDefinition = "TEXT")
    private String targetCountries;

    @Column(name = "min_employees")
    @Builder.Default
    private Integer minEmployees = 1;

    @Column(name = "max_employees")
    @Builder.Default
    private Integer maxEmployees = 1000;

    @Column(name = "target_job_titles", columnDefinition = "TEXT")
    private String targetJobTitles;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "excluded_industries", columnDefinition = "TEXT")
    private String excludedIndustries;
}
