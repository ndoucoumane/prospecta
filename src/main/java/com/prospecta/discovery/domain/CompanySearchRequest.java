package com.prospecta.discovery.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record CompanySearchRequest(
        String name,
        String domain,
        String industry,
        String country,
        String city,
        Integer companySizeMin,
        Integer companySizeMax,
        @Min(value = 0, message = "La page doit être supérieure ou égale à 0")
        int page,
        @Min(value = 1, message = "La taille minimale par page est 1")
        @Max(value = 50, message = "La taille maximale par page est 50 pour préserver les quotas et performances")
        int size
) {
    public CompanySearchRequest {
        if (size <= 0) {
            size = 25;
        }
        if (page < 0) {
            page = 0;
        }
    }
}
