package com.prospecta.discovery.domain;

import lombok.Builder;

@Builder
public record DiscoveredCompany(
        String externalId,
        String name,
        String domain,
        String industry,
        String country,
        String city,
        Integer employeeCount,
        String linkedinUrl,
        String source
) {}
