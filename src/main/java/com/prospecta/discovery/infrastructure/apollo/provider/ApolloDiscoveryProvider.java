package com.prospecta.discovery.infrastructure.apollo.provider;

import com.prospecta.discovery.domain.*;
import com.prospecta.discovery.infrastructure.apollo.client.ApolloClient;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloOrgSearchRequest;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloOrgSearchResponse;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPeopleSearchRequest;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPeopleSearchResponse;
import com.prospecta.discovery.infrastructure.apollo.mapper.ApolloMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApolloDiscoveryProvider implements DiscoveryProvider {

    private final ApolloClient apolloClient;
    private final ApolloMapper apolloMapper;

    @Override
    public PeopleSearchResult searchPeople(PeopleSearchRequest request) {
        log.debug("ApolloDiscoveryProvider: Recherche de personnes - filtres: [titres={}, pays={}, ville={}, entreprise={}]",
                request.jobTitles(), request.country(), request.city(), request.companyName());

        ApolloPeopleSearchRequest apolloRequest = apolloMapper.toApolloRequest(request);
        ApolloPeopleSearchResponse apolloResponse = apolloClient.searchPeople(apolloRequest);

        return apolloMapper.toPeopleSearchResult(apolloResponse, request.page(), request.size());
    }

    @Override
    public CompanySearchResult searchCompanies(CompanySearchRequest request) {
        log.debug("ApolloDiscoveryProvider: Recherche d'entreprises - filtres: [nom={}, domaine={}, pays={}, ville={}]",
                request.name(), request.domain(), request.country(), request.city());

        ApolloOrgSearchRequest apolloRequest = apolloMapper.toApolloRequest(request);
        ApolloOrgSearchResponse apolloResponse = apolloClient.searchOrganizations(apolloRequest);

        return apolloMapper.toCompanySearchResult(apolloResponse, request.page(), request.size());
    }

    @Override
    public String getProviderName() {
        return "APOLLO";
    }
}
