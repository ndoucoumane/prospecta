package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.DiscoveredPerson;
import com.prospecta.discovery.infrastructure.persistence.LeadList;
import com.prospecta.discovery.web.dto.ImportPeopleReport;
import com.prospecta.discovery.web.dto.ImportPeopleRequest;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.prospect.service.DeduplicationService;
import com.prospecta.prospect.service.LeadScoringEngine;
import com.prospecta.shared.audit.AuditService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryImportServiceTest {

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private LeadScoringEngine leadScoringEngine;

    @Mock
    private LeadListService leadListService;

    @Mock
    private DiscoveryResultCache discoveryResultCache;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DiscoveryImportService importService;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        TenantContextHolder.setContext(TenantContext.of(orgId, null, "trace-test"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should successfully import new discovered prospects, deduplicate, score, and link to list")
    void shouldImportNewProspectsSuccessfully() {
        UUID listId = UUID.randomUUID();

        DiscoveredPerson person = DiscoveredPerson.builder()
                .externalId("apollo-ext-100")
                .firstName("Mamadou")
                .lastName("Diop")
                .jobTitle("Directeur commercial")
                .companyName("Gainde 2000")
                .companyDomain("gainde2000.sn")
                .email("mamadou.diop@gainde2000.sn")
                .phoneNumber("+221776543210")
                .source("APOLLO")
                .build();

        ImportPeopleRequest request = ImportPeopleRequest.builder()
                .prospects(List.of(person))
                .listId(listId)
                .build();

        when(prospectRepository.findByOrganizationIdAndExternalId(orgId, "apollo-ext-100"))
                .thenReturn(Optional.empty());
        when(deduplicationService.findDuplicate(eq(orgId), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(companyRepository.findByOrganizationIdAndNameIgnoreCase(orgId, "Gainde 2000"))
                .thenReturn(Optional.empty());
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeadList mockList = LeadList.builder().name("Liste Test").build();
        mockList.setId(listId);
        when(leadListService.getLeadList(listId)).thenReturn(mockList);

        Prospect savedMock = Prospect.builder()
                .firstName("Mamadou")
                .lastName("Diop")
                .externalId("apollo-ext-100")
                .build();
        savedMock.setId(UUID.randomUUID());
        savedMock.setOrganizationId(orgId);

        when(leadScoringEngine.score(any(Prospect.class))).thenReturn(
                com.prospecta.prospect.dto.LeadScoreResult.builder().score(50).level(com.prospecta.prospect.domain.LeadScoreLevel.MEDIUM).reasons(List.of("Test")).build()
        );
        when(prospectRepository.save(any(Prospect.class))).thenReturn(savedMock);

        ImportPeopleReport report = importService.importPeople(request);

        assertThat(report.importedCount()).isEqualTo(1);
        assertThat(report.duplicateCount()).isEqualTo(0);
        assertThat(report.totalProcessed()).isEqualTo(1);
        assertThat(report.prospectIds()).containsExactly(savedMock.getId());

        verify(leadScoringEngine).score(any(Prospect.class));
        verify(leadListService).addProspectsToList(eq(listId), eq(List.of(savedMock.getId())));
        verify(auditService).logSync(eq("IMPORT_DISCOVERY"), eq("PROSPECT"), eq(savedMock.getId().toString()), anyString());
    }

    @Test
    @DisplayName("Should detect duplicate prospect and not create redundant record")
    void shouldDetectDuplicateAndSkipCreation() {
        DiscoveredPerson person = DiscoveredPerson.builder()
                .externalId("apollo-ext-duplicate")
                .firstName("Fatou")
                .lastName("Ndiaye")
                .email("fatou.ndiaye@wave.com")
                .build();

        ImportPeopleRequest request = ImportPeopleRequest.builder()
                .prospects(List.of(person))
                .build();

        Prospect existing = Prospect.builder()
                .firstName("Fatou")
                .lastName("Ndiaye")
                .externalId("apollo-ext-duplicate")
                .build();
        existing.setId(UUID.randomUUID());
        existing.setOrganizationId(orgId);

        when(prospectRepository.findByOrganizationIdAndExternalId(orgId, "apollo-ext-duplicate"))
                .thenReturn(Optional.of(existing));

        ImportPeopleReport report = importService.importPeople(request);

        assertThat(report.importedCount()).isEqualTo(0);
        assertThat(report.duplicateCount()).isEqualTo(1);
        assertThat(report.prospectIds()).containsExactly(existing.getId());

        verify(prospectRepository, never()).save(any());
    }
}
