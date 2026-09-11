package com.prospecta.discovery.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record CompanySearchResult(
        List<DiscoveredCompany> items,
        int page,
        int size,
        long total,
        String source
) {
    public CompanySearchResult {
        if (items == null) {
            items = List.of();
        }
        if (source == null) {
            source = "APOLLO";
        }
    }

    public static CompanySearchResult empty(int page, int size, String source) {
        return new CompanySearchResult(List.of(), page, size, 0L, source);
    }
}
