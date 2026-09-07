package com.prospecta.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.ai.domain.AiPromptTemplate;
import com.prospecta.ai.dto.AiDto.AiResponse;
import com.prospecta.ai.dto.AiDto.CompanyAnalysisResult;
import com.prospecta.ai.provider.AiProvider;
import com.prospecta.ai.repository.AiPromptTemplateRepository;
import com.prospecta.enrichment.service.WebsiteResearchService;
import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.repository.CompanyRepository;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyAnalysisServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private WebsiteResearchService websiteResearchService;

    @Mock
    private AiPromptTemplateRepository promptTemplateRepository;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private QuotaService quotaService;

    @Mock
    private AuditService auditService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CompanyAnalysisService companyAnalysisService;

    private UUID organizationId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .organizationId(organizationId)
                .email("admin@prospecta.sn")
                .permissions(Collections.emptySet())
                .build();
        TenantContextHolder.setContext(TenantContext.of(organizationId, principal, "trace-ai-1"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should fetch website, call AI provider, parse structured JSON, and update company profile")
    void shouldAnalyzeCompanySuccessfully() {
        Company company = Company.builder()
                .name("Teranga Tech")
                .website("https://terangatech.sn")
                .industry("IT_SERVICES")
                .build();
        company.setId(companyId);
        company.setOrganizationId(organizationId);

        when(companyRepository.findByIdAndOrganizationId(companyId, organizationId))
                .thenReturn(Optional.of(company));
        when(websiteResearchService.fetchCleanWebsiteContent("https://terangatech.sn"))
                .thenReturn(Optional.of("Solutions logicielles pour PME à Dakar."));

        AiPromptTemplate template = AiPromptTemplate.builder()
                .name("COMPANY_ANALYSIS_V1")
                .systemPrompt("System prompt")
                .userPromptTemplate("Name: {companyName}, Web: {website}, Content: {content}")
                .active(true)
                .build();
        when(promptTemplateRepository.findByNameAndActiveTrue("COMPANY_ANALYSIS_V1"))
                .thenReturn(Optional.of(template));

        String structuredJson = """
                {
                  "summary": "Leader de l'intégration logicielle à Dakar",
                  "industry": "IT_SERVICES",
                  "painPoints": ["Manque de prospection sortante", "Cycles de vente longs"],
                  "opportunities": ["Automatisation WhatsApp"],
                  "recommendedApproach": "Démonstration personnalisée",
                  "confidence": 0.92
                }
                """;

        AiResponse aiResponse = AiResponse.builder()
                .content(structuredJson)
                .model("gpt-4o-mini")
                .inputTokens(300)
                .outputTokens(150)
                .durationMs(450)
                .success(true)
                .build();
        when(aiProvider.generate(any())).thenReturn(aiResponse);
        when(aiProvider.getProviderName()).thenReturn("openai");

        CompanyAnalysisResult result = companyAnalysisService.analyzeCompany(companyId);

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("Leader de l'intégration logicielle à Dakar");
        assertThat(result.getPainPoints()).contains("Manque de prospection sortante");
        assertThat(result.getConfidence()).isEqualTo(0.92);

        verify(quotaService).checkQuota(organizationId);
        verify(quotaService).recordUsage(eq(organizationId), any(), eq("openai"), eq("gpt-4o-mini"), eq("COMPANY_ANALYSIS"), eq(300), eq(150), eq(450L));
        verify(companyRepository).save(company);
        verify(auditService).logSync(eq("COMPANY_AI_ANALYZED"), eq("Company"), eq(companyId.toString()), any());
    }
}
