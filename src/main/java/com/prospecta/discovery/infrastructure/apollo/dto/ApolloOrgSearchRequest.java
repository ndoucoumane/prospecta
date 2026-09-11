package com.prospecta.discovery.infrastructure.apollo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApolloOrgSearchRequest {

    @JsonProperty("q_organization_name")
    private String qOrganizationName;

    @JsonProperty("q_organization_domains")
    private List<String> qOrganizationDomains;

    @JsonProperty("organization_locations")
    private List<String> organizationLocations;

    @JsonProperty("organization_num_employees_ranges")
    private List<String> organizationNumEmployeesRanges;

    @JsonProperty("page")
    private int page;

    @JsonProperty("per_page")
    private int perPage;
}
