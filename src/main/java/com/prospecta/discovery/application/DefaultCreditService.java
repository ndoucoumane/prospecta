package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.CreditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Default implementation of CreditService for the MVP.
 * Acts as a clean extension point for billing quotas and credits.
 */
@Slf4j
@Service
public class DefaultCreditService implements CreditService {

    @Override
    public void reserveDiscoveryCredits(UUID organizationId, int count) {
        log.debug("Discovery credits reserved for organization [{}]: {} credits", organizationId, count);
    }

    @Override
    public void consumeDiscoveryCredits(UUID organizationId, int count) {
        log.info("Discovery credits consumed for organization [{}]: {} credits", organizationId, count);
    }

    @Override
    public void refundDiscoveryCredits(UUID organizationId, int count) {
        log.debug("Discovery credits refunded for organization [{}]: {} credits", organizationId, count);
    }
}
