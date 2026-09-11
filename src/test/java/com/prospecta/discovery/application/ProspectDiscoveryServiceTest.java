package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.*;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProspectDiscoveryServiceTest {

    @Mock
    private DiscoveryProvider discoveryProvider;

    @Mock
    private CreditService creditService;

    @Mock
    private DiscoveryResultCache discoveryResultCache;

    private SimpleMeterRegistry meterRegistry;
    private ProspectDiscoveryService service;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        TenantContextHolder.setContext(TenantContext.of(orgId, null, "trace-test"));

        meterRegistry = new SimpleMeterRegistry();
        service = new ProspectDiscoveryService(discoveryProvider, creditService, discoveryResultCache, meterRegistry);

        when(discoveryProvider.getProviderName()).thenReturn("APOLLO");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should execute people search, reserve/consume credits, record metrics, and cache results")
    void shouldExecutePeopleSearchSuccessfully() {
        PeopleSearchRequest request = PeopleSearchRequest.builder()
                .jobTitles(List.of("CEO"))
                .country("SN")
                .city("Dakar")
                .page(0)
                .size(25)
                .build();

        DiscoveredPerson person = DiscoveredPerson.builder()
                .externalId("apollo-1")
                .firstName("Mamadou")
                .lastName("Diop")
                .jobTitle("CEO")
                .build();

        PeopleSearchResult expectedResult = PeopleSearchResult.builder()
                .items(List.of(person))
                .page(0)
                .size(25)
                .total(1L)
                .source("APOLLO")
                .build();

        when(discoveryProvider.searchPeople(request)).thenReturn(expectedResult);

        PeopleSearchResult result = service.searchPeople(request);

        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).firstName()).isEqualTo("Mamadou");

        // Verify credit lifecycle
        verify(creditService).reserveDiscoveryCredits(orgId, 25);
        verify(creditService).consumeDiscoveryCredits(orgId, 1);
        verify(discoveryResultCache).putAll(eq(orgId), any());

        // Verify metrics
        assertThat(meterRegistry.find("discovery_requests_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("discovery_success_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("discovery_results_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should refund credits and record error metric when provider throws an exception")
    void shouldRefundCreditsOnProviderError() {
        PeopleSearchRequest request = PeopleSearchRequest.builder()
                .country("SN")
                .page(0)
                .size(10)
                .build();

        when(discoveryProvider.searchPeople(request))
                .thenThrow(new DiscoveryException(DiscoveryErrorCode.PROVIDER_UNAVAILABLE));

        assertThatThrownBy(() -> service.searchPeople(request))
                .isInstanceOf(DiscoveryException.class);

        verify(creditService).reserveDiscoveryCredits(orgId, 10);
        verify(creditService).refundDiscoveryCredits(orgId, 10);
        verify(creditService, never()).consumeDiscoveryCredits(any(), anyInt());

        assertThat(meterRegistry.find("discovery_errors_total").counter().count()).isEqualTo(1.0);
    }
}
