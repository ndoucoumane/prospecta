package com.prospecta.discovery.domain;

public interface CompanyDiscoveryProvider {

    CompanySearchResult searchCompanies(CompanySearchRequest request);

    String getProviderName();
}
