package com.prospecta.discovery.infrastructure.apollo.client;

import com.prospecta.discovery.infrastructure.apollo.dto.*;

public interface ApolloClient {

    ApolloPeopleSearchResponse searchPeople(ApolloPeopleSearchRequest request);

    ApolloOrgSearchResponse searchOrganizations(ApolloOrgSearchRequest request);

    ApolloPersonMatchResponse matchPerson(ApolloPersonMatchRequest request);
}
