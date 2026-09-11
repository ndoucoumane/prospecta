package com.prospecta.discovery.infrastructure.apollo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApolloOrganization {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("website_url")
    private String websiteUrl;

    @JsonProperty("primary_domain")
    private String primaryDomain;

    @JsonProperty("linkedin_url")
    private String linkedinUrl;

    @JsonProperty("twitter_url")
    private String twitterUrl;

    @JsonProperty("facebook_url")
    private String facebookUrl;

    @JsonProperty("industry")
    private String industry;

    @JsonProperty("estimated_num_employees")
    private Integer estimatedNumEmployees;

    @JsonProperty("country")
    private String country;

    @JsonProperty("city")
    private String city;

    @JsonProperty("phone")
    private String phone;
}
