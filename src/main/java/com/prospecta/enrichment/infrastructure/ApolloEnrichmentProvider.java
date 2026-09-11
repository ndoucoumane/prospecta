package com.prospecta.enrichment.infrastructure;

import com.prospecta.discovery.infrastructure.apollo.client.ApolloClient;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPerson;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPersonMatchRequest;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPersonMatchResponse;
import com.prospecta.enrichment.domain.EnrichedProspectData;
import com.prospecta.enrichment.domain.EnrichmentProvider;
import com.prospecta.prospect.domain.Prospect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApolloEnrichmentProvider implements EnrichmentProvider {

    private final ApolloClient apolloClient;

    @Override
    public Optional<EnrichedProspectData> enrichPerson(Prospect prospect) {
        if (prospect == null) {
            return Optional.empty();
        }

        ApolloPersonMatchRequest matchRequest = ApolloPersonMatchRequest.builder()
                .id(prospect.getExternalId())
                .firstName(prospect.getFirstName())
                .lastName(prospect.getLastName())
                .name(prospect.getFullName())
                .organizationName(prospect.getCompanyName())
                .domain(prospect.getCompanyWebsite())
                .revealPersonalEmails(true)
                .revealPhoneNumber(true)
                .build();

        log.debug("ApolloEnrichmentProvider: Début de l'enrichissement pour prospect [{}] (externalId: {})",
                prospect.getFullName(), prospect.getExternalId());

        ApolloPersonMatchResponse response = apolloClient.matchPerson(matchRequest);
        if (response == null) {
            return Optional.empty();
        }

        ApolloPerson matched = response.getEffectivePerson();
        if (matched == null) {
            return Optional.empty();
        }

        String phone = null;
        String mobile = null;
        if (matched.getPhoneNumbers() != null) {
            for (ApolloPerson.ApolloPhoneNumber p : matched.getPhoneNumbers()) {
                String num = p.getSanitizedNumber() != null ? p.getSanitizedNumber() : p.getRawNumber();
                if ("mobile".equalsIgnoreCase(p.getType()) && mobile == null) {
                    mobile = num;
                } else if (phone == null) {
                    phone = num;
                }
            }
        }
        if (phone == null && mobile != null) {
            phone = mobile;
        }

        String companyName = matched.getOrganization() != null ? matched.getOrganization().getName() : prospect.getCompanyName();
        String companyDomain = matched.getOrganization() != null && matched.getOrganization().getPrimaryDomain() != null
                ? matched.getOrganization().getPrimaryDomain()
                : prospect.getCompanyWebsite();

        EnrichedProspectData enriched = EnrichedProspectData.builder()
                .email(matched.getEmail())
                .phone(phone)
                .mobilePhone(mobile)
                .linkedinUrl(matched.getLinkedinUrl())
                .companyName(companyName)
                .companyDomain(companyDomain)
                .jobTitle(matched.getTitle())
                .city(matched.getCity())
                .country(matched.getCountry())
                .build();

        return Optional.of(enriched);
    }

    @Override
    public String getProviderName() {
        return "APOLLO";
    }
}
