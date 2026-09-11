package com.prospecta.discovery.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record PeopleSearchResult(
        List<DiscoveredPerson> items,
        int page,
        int size,
        long total,
        String source
) {
    public PeopleSearchResult {
        if (items == null) {
            items = List.of();
        }
        if (source == null) {
            source = "APOLLO";
        }
    }

    public static PeopleSearchResult empty(int page, int size, String source) {
        return new PeopleSearchResult(List.of(), page, size, 0L, source);
    }
}
