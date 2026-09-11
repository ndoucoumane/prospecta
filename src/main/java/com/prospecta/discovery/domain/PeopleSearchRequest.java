package com.prospecta.discovery.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.List;

@Builder
public record PeopleSearchRequest(
        String firstName,
        String lastName,
        List<String> jobTitles,
        String companyName,
        String companyDomain,
        String country,
        String city,
        String industry,
        Integer companySizeMin,
        Integer companySizeMax,
        @Min(value = 0, message = "La page doit être supérieure ou égale à 0")
        int page,
        @Min(value = 1, message = "La taille minimale par page est 1")
        @Max(value = 50, message = "La taille maximale par page est 50 pour préserver les quotas et performances")
        int size
) {
    public PeopleSearchRequest {
        if (size <= 0) {
            size = 25;
        }
        if (page < 0) {
            page = 0;
        }
        if (jobTitles == null) {
            jobTitles = List.of();
        }
    }
}
