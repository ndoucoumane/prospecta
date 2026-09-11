package com.prospecta.enrichment.service;

import com.prospecta.enrichment.domain.EnrichedProspectData;
import com.prospecta.enrichment.domain.EnrichmentProvider;
import com.prospecta.prospect.domain.LeadScoreLevel;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.dto.ProspectResponse;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.prospect.service.LeadScoringEngine;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProspectEnrichmentServiceTest {

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private EnrichmentProvider enrichmentProvider;

    @Mock
    private LeadScoringEngine leadScoringEngine;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProspectEnrichmentService enrichmentService;

    private UUID orgId;
    private UUID prospectId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        prospectId = UUID.randomUUID();
        TenantContextHolder.setContext(TenantContext.of(orgId, null, "trace-test"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should successfully enrich prospect with email, phone, and recalculate lead score")
    void shouldEnrichProspectSuccessfully() {
        Prospect initial = Prospect.builder()
                .firstName("Mamadou")
                .lastName("Diop")
                .fullName("Mamadou Diop")
                .jobTitle("CEO")
                .companyName("ABC Sénégal")
                .source("APOLLO")
                .status(ProspectStatus.NEW)
                .build();
        initial.setId(prospectId);
        initial.setOrganizationId(orgId);

        when(prospectRepository.findByIdAndOrganizationId(prospectId, orgId))
                .thenReturn(Optional.of(initial));

        EnrichedProspectData enrichedData = EnrichedProspectData.builder()
                .email("mamadou.diop@abc.sn")
                .phone("+221776543210")
                .mobilePhone("+221776543210")
                .linkedinUrl("https://linkedin.com/in/mamadou-diop")
                .companyDomain("abc.sn")
                .build();

        when(enrichmentProvider.enrichPerson(initial))
                .thenReturn(Optional.of(enrichedData));
        when(enrichmentProvider.getProviderName())
                .thenReturn("APOLLO");

        when(leadScoringEngine.score(any(Prospect.class))).thenReturn(
                LeadScoreResult.builder()
                        .score(85)
                        .level(LeadScoreLevel.VERY_HIGH)
                        .reasons(List.of("Email vérifié", "WhatsApp sénégalais valide (+221)"))
                        .build()
        );

        when(prospectRepository.save(any(Prospect.class))).thenAnswer(inv -> inv.getArgument(0));

        ProspectResponse response = enrichmentService.enrichProspect(prospectId);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("mamadou.diop@abc.sn");
        assertThat(response.getPhone()).isEqualTo("+221776543210");
        assertThat(response.getWhatsappNumber()).isEqualTo("+221776543210");
        assertThat(response.getLeadScore()).isEqualTo(85);
        assertThat(response.getLeadScoreLevel()).isEqualTo(LeadScoreLevel.VERY_HIGH);
        assertThat(response.getStatus()).isEqualTo(ProspectStatus.QUALIFIED);

        verify(auditService).logSync(eq("ENRICHED"), eq("PROSPECT"), eq(prospectId.toString()), anyString());
    }

    @Test
    @DisplayName("Should throw ProspectNotFoundException if prospect belongs to another tenant")
    void shouldEnforceTenantIsolation() {
        when(prospectRepository.findByIdAndOrganizationId(prospectId, orgId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrichmentService.enrichProspect(prospectId))
                .isInstanceOf(ProspectNotFoundException.class);

        verify(enrichmentProvider, never()).enrichPerson(any());
    }
}
