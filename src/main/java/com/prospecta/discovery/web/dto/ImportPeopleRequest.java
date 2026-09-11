package com.prospecta.discovery.web.dto;

import com.prospecta.discovery.domain.DiscoveredPerson;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ImportPeopleRequest(
        List<DiscoveredPerson> prospects,
        List<String> externalIds,
        UUID listId,
        String listName
) {}
