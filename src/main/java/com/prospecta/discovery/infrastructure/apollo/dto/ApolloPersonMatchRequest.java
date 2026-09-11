package com.prospecta.discovery.infrastructure.apollo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApolloPersonMatchRequest {

    @JsonProperty("id")
    private String id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("reveal_personal_emails")
    private Boolean revealPersonalEmails;

    @JsonProperty("reveal_phone_number")
    private Boolean revealPhoneNumber;
}
