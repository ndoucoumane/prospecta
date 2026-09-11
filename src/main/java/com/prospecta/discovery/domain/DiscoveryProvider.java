package com.prospecta.discovery.domain;

/**
 * Unified Discovery Provider abstraction.
 * Allows switching or combining multiple B2B data providers (Apollo, etc.)
 * without modifying domain logic.
 */
public interface DiscoveryProvider extends PeopleDiscoveryProvider, CompanyDiscoveryProvider {

    @Override
    default String getProviderName() {
        return "APOLLO";
    }
}
