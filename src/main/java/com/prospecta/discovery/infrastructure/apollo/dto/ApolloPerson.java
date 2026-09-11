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
public class ApolloPerson {

    @JsonProperty("id")
    private String id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("title")
    private String title;

    @JsonProperty("headline")
    private String headline;

    @JsonProperty("linkedin_url")
    private String linkedinUrl;

    @JsonProperty("organization_id")
    private String organizationId;

    @JsonProperty("organization")
    private ApolloOrganization organization;

    @JsonProperty("country")
    private String country;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone_numbers")
    private List<ApolloPhoneNumber> phoneNumbers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApolloPhoneNumber {
        @JsonProperty("raw_number")
        private String rawNumber;

        @JsonProperty("sanitized_number")
        private String sanitizedNumber;

        @JsonProperty("type")
        private String type;
    }
}
