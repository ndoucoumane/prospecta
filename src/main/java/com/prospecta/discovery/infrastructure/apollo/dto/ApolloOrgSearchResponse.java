package com.prospecta.discovery.infrastructure.apollo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApolloOrgSearchResponse {

    @JsonProperty("organizations")
    private List<ApolloOrganization> organizations;

    @JsonProperty("pagination")
    private ApolloPagination pagination;

    @JsonProperty("total_entries")
    private Long totalEntries;

    public long resolveTotalEntries() {
        if (totalEntries != null) {
            return totalEntries;
        }
        if (pagination != null) {
            return pagination.getTotalEntries();
        }
        return organizations != null ? organizations.size() : 0L;
    }
}
