package com.prospecta.discovery.web.dto;

import com.prospecta.discovery.infrastructure.persistence.LeadList;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LeadListResponse(
        UUID id,
        String name,
        String description,
        long memberCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static LeadListResponse fromEntity(LeadList list, long memberCount) {
        return LeadListResponse.builder()
                .id(list.getId())
                .name(list.getName())
                .description(list.getDescription())
                .memberCount(memberCount)
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }
}
