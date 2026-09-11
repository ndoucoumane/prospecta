package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.CompanySearchRequest;
import com.prospecta.discovery.domain.CompanySearchResult;
import com.prospecta.discovery.domain.DiscoveryProvider;
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
public class CompanyDiscoveryService {

    private final DiscoveryProvider discoveryProvider;
    private final MeterRegistry meterRegistry;

    public CompanyDiscoveryService(DiscoveryProvider discoveryProvider, MeterRegistry meterRegistry) {
        this.discoveryProvider = discoveryProvider;
        this.meterRegistry = meterRegistry;
    }

    public CompanySearchResult searchCompanies(CompanySearchRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        String providerName = discoveryProvider.getProviderName();
        Instant start = Instant.now();

        Counter.builder("discovery_requests_total")
                .tag("type", "companies")
                .tag("provider", providerName)
                .register(meterRegistry)
                .increment();

        log.info("discovery.start provider={} type=companies tenantId={} page={} size={} name={} domain={} country={} city={}",
                providerName, organizationId, request.page(), request.size(),
                request.name(), request.domain(), request.country(), request.city());

        try {
            CompanySearchResult result = discoveryProvider.searchCompanies(request);
            long durationMs = Duration.between(start, Instant.now()).toMillis();

            Counter.builder("discovery_success_total")
                    .tag("type", "companies")
                    .tag("provider", providerName)
                    .register(meterRegistry)
                    .increment();

            Counter.builder("discovery_results_total")
                    .tag("type", "companies")
                    .tag("provider", providerName)
                    .register(meterRegistry)
                    .increment(result.items() != null ? result.items().size() : 0);

            Timer.builder("discovery_duration")
                    .tag("type", "companies")
                    .tag("provider", providerName)
                    .register(meterRegistry)
                    .record(Duration.ofMillis(durationMs));

            log.info("discovery.success provider={} type=companies tenantId={} durationMs={} resultCount={} totalCount={}",
                    providerName, organizationId, durationMs,
                    result.items() != null ? result.items().size() : 0, result.total());

            return result;

        } catch (RuntimeException ex) {
            Counter.builder("discovery_errors_total")
                    .tag("type", "companies")
                    .tag("provider", providerName)
                    .tag("error", ex.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();

            log.error("discovery.error provider={} type=companies tenantId={} error={}",
                    providerName, organizationId, ex.getMessage());
            throw ex;
        }
    }
}
