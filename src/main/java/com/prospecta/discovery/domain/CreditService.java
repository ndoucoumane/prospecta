package com.prospecta.discovery.domain;

import java.util.UUID;

/**
 * Clean extension point for quota and credit management.
 * Avoids hardcoding vendor-specific pricing in domain logic.
 */
public interface CreditService {

    void reserveDiscoveryCredits(UUID organizationId, int count);

    void consumeDiscoveryCredits(UUID organizationId, int count);

    void refundDiscoveryCredits(UUID organizationId, int count);
}
