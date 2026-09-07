package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.LeadScoreLevel;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.dto.ImportProspectsReport;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProspectImportServiceTest {

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private LeadScoringEngine leadScoringEngine;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProspectImportService prospectImportService;

    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .organizationId(organizationId)
                .email("admin@prospecta.sn")
                .permissions(Collections.emptySet())
                .build();
        TenantContextHolder.setContext(TenantContext.of(organizationId, principal, "trace-import-1"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("CSV Import should process file, normalize phones, detect duplicates, and score valid prospects")
    void shouldProcessCsvImportCorrectly() {
        String csvContent = "firstName,lastName,email,phone,companyName,jobTitle,city,industry\n" +
                "Awa,Ndiaye,awa@teranga.sn,771234567,Teranga Hotel,Directrice Commerciale,Dakar,HOSPITALITY\n" +
                "Fatou,Diop,fatou@existing.sn,789876543,Existing Corp,CEO,Dakar,TECH\n" +
                "Incomplete,NoContact,,,No Contact Corp,Dev,Dakar,TECH\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "prospects.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Row 1: New
        when(deduplicationService.findDuplicate(eq(organizationId), eq("awa@teranga.sn"), eq("+221771234567"), eq("+221771234567"), eq("Awa"), eq("Ndiaye"), eq("Teranga Hotel")))
                .thenReturn(Optional.empty());

        // Row 2: Duplicate
        when(deduplicationService.findDuplicate(eq(organizationId), eq("fatou@existing.sn"), eq("+221789876543"), eq("+221789876543"), eq("Fatou"), eq("Diop"), eq("Existing Corp")))
                .thenReturn(Optional.of(Prospect.builder().email("fatou@existing.sn").build()));

        when(leadScoringEngine.score(any(Prospect.class)))
                .thenReturn(LeadScoreResult.builder()
                        .score(90)
                        .level(LeadScoreLevel.VERY_HIGH)
                        .reasons(List.of("Dakar", "WhatsApp"))
                        .build());

        ImportProspectsReport report = prospectImportService.importCsv(file);

        assertThat(report).isNotNull();
        assertThat(report.getTotal()).isEqualTo(3);
        assertThat(report.getCreated()).isEqualTo(1);
        assertThat(report.getDuplicates()).isEqualTo(1);
        assertThat(report.getInvalid()).isEqualTo(1);

        verify(prospectRepository, times(1)).save(any(Prospect.class));
        verify(auditService).logSync(eq("PROSPECTS_IMPORTED"), eq("Prospect"), any(), any());
    }
}
