package com.prospecta.discovery.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateLeadListRequest(
        @NotBlank(message = "Le nom de la liste est obligatoire")
        String name,
        String description
) {}
