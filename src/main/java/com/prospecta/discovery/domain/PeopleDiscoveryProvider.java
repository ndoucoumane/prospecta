package com.prospecta.discovery.domain;

public interface PeopleDiscoveryProvider {

    PeopleSearchResult searchPeople(PeopleSearchRequest request);

    String getProviderName();
}
