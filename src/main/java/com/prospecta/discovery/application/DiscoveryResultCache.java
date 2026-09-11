package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.DiscoveredPerson;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary in-memory cache of discovered persons by tenant.
 * Enables selective import by externalId without immediately saving all search results.
 */
@Component
public class DiscoveryResultCache {

    // Key: orgId + ":" + externalId
    private final Map<String, DiscoveredPerson> cache = new ConcurrentHashMap<>();

    public void put(UUID organizationId, DiscoveredPerson person) {
        if (organizationId != null && person != null && person.externalId() != null) {
            cache.put(buildKey(organizationId, person.externalId()), person);
        }
    }

    public void putAll(UUID organizationId, List<DiscoveredPerson> people) {
        if (organizationId != null && people != null) {
            for (DiscoveredPerson person : people) {
                put(organizationId, person);
            }
        }
    }

    public Optional<DiscoveredPerson> get(UUID organizationId, String externalId) {
        if (organizationId == null || externalId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(buildKey(organizationId, externalId)));
    }

    public void remove(UUID organizationId, String externalId) {
        if (organizationId != null && externalId != null) {
            cache.remove(buildKey(organizationId, externalId));
        }
    }

    private String buildKey(UUID organizationId, String externalId) {
        return organizationId + ":" + externalId.trim();
    }
}
