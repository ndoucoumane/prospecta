package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.CreditService;
import com.prospecta.discovery.domain.DiscoveryProvider;
import com.prospecta.discovery.domain.PeopleSearchRequest;
import com.prospecta.discovery.domain.PeopleSearchResult;
import com.prospecta.shared.security.TenantContextHolder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ProspectDiscoveryService {

    private final DiscoveryProvider discoveryProvider;
    private final CreditService creditService;
    private final DiscoveryResultCache discoveryResultCache;
    private final MeterRegistry meterRegistry;

    public ProspectDiscoveryService(
            DiscoveryProvider discoveryProvider,
            CreditService creditService,
            DiscoveryResultCache discoveryResultCache,
            MeterRegistry meterRegistry
    ) {
        this.discoveryProvider = discoveryProvider;
        this.creditService = creditService;
        this.discoveryResultCache = discoveryResultCache;
        this.meterRegistry = meterRegistry;
    }

    public PeopleSearchResult searchPeople(PeopleSearchRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        String providerName = discoveryProvider.getProviderName();
        Instant start = Instant.now();

        Counter.builder("discovery_requests_total")
                .tag("type", "people")
                .tag("provider", providerName)
                .register(meterRegistry)
                .increment();

        log.info("discovery.start provider={} type=people tenantId={} page={} size={} jobTitles={} country={} city={}",
                providerName, organizationId, request.page(), request.size(),
                request.jobTitles(), request.country(), request.city());

        creditService.reserveDiscoveryCredits(organizationId, request.size());

        try {
            PeopleSearchResult result = discoveryProvider.searchPeople(request);
            long durationMs = Duration.between(start, Instant.now()).toMillis();

            // Cache discovered people for selective import
            if (result.items() != null && !result.items().isEmpty()) {
                discoveryResultCache.putAll(organizationId, result.items());
            }

            creditService.consumeDiscoveryCredits(organizationId, result.items() != null ? result.items().size() : 0);

            Counter.builder("discovery_success_total")
                    .tag("type", "people")
                    .tag("provider", providerName)
                    .register(meterRegistry)
                    .increment();

            Counter.builder("discovery_results_total")
                    .tag("type", "people")
                    .tag("provider", providerName)
                    .register(meterRegistry)
                    .increment(result.items() != null ? result.items().size() : 0);

            Timer.builder("discovery_duration")
                    .tag("type", "people")
                    .tag("provider", providerName)
                    .register(meterRegistry)
                    .record(Duration.ofMillis(durationMs));

            log.info("discovery.success provider={} type=people tenantId={} durationMs={} resultCount={} totalCount={}",
                    providerName, organizationId, durationMs,
                    result.items() != null ? result.items().size() : 0, result.total());

            return result;

        } catch (RuntimeException ex) {
            creditService.refundDiscoveryCredits(organizationId, request.size());

            Counter.builder("discovery_errors_total")
                    .tag("type", "people")
                    .tag("provider", providerName)
                    .tag("error", ex.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();

            log.error("discovery.error provider={} type=people tenantId={} error={}",
                    providerName, organizationId, ex.getMessage());
            throw ex;
        }
    }
}
