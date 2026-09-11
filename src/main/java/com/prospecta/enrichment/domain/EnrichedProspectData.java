package com.prospecta.enrichment.domain;

import lombok.Builder;

@Builder
public record EnrichedProspectData(
        String email,
        String phone,
        String mobilePhone,
        String linkedinUrl,
        String companyName,
        String companyDomain,
        String jobTitle,
        String city,
        String country
) {}
