package com.prospecta.discovery.domain;

import lombok.Builder;

@Builder
public record DiscoveredPerson(
        String externalId,
        String firstName,
        String lastName,
        String jobTitle,
        String companyName,
        String companyDomain,
        String linkedinUrl,
        String country,
        String city,
        String email,
        String phoneNumber,
        String source
) {
    public String fullName() {
        if (firstName == null && lastName == null) {
            return "Inconnu";
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}
