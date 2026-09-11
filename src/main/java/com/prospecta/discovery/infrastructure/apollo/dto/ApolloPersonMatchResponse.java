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
public class ApolloPersonMatchResponse {

    @JsonProperty("person")
    private ApolloPerson person;

    @JsonProperty("matches")
    private List<ApolloPerson> matches;

    public ApolloPerson getEffectivePerson() {
        if (person != null) {
            return person;
        }
        if (matches != null && !matches.isEmpty()) {
            return matches.get(0);
        }
        return null;
    }
}
