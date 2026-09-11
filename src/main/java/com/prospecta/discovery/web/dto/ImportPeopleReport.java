package com.prospecta.discovery.web.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ImportPeopleReport(
        int importedCount,
        int duplicateCount,
        int totalProcessed,
        UUID listId,
        String listName,
        List<UUID> prospectIds
) {}
