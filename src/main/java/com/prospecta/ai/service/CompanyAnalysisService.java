package com.prospecta.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.ai.domain.AiPromptTemplate;
import com.prospecta.ai.dto.AiDto.AiRequest;
import com.prospecta.ai.dto.AiDto.AiResponse;
import com.prospecta.ai.dto.AiDto.CompanyAnalysisResult;
import com.prospecta.ai.provider.AiProvider;
import com.prospecta.ai.repository.AiPromptTemplateRepository;
import com.prospecta.enrichment.service.WebsiteResearchService;
import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.CompanyNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAnalysisService {

    private final CompanyRepository companyRepository;
    private final WebsiteResearchService websiteResearchService;
    private final AiPromptTemplateRepository promptTemplateRepository;
    private final AiProvider aiProvider;
    private final QuotaService quotaService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CompanyAnalysisResult analyzeCompany(UUID companyId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        UUID userId = TenantContextHolder.getUserPrincipal().map(UserPrincipal::getUserId).orElse(null);

        // 1. Quota Check
        quotaService.checkQuota(organizationId);

        Company company = companyRepository.findByIdAndOrganizationId(companyId, organizationId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));

        // 2. Fetch clean website text
        String siteContent = "Aucun contenu web accessible.";
        if (company.getWebsite() != null && !company.getWebsite().isBlank()) {
            Optional<String> fetched = websiteResearchService.fetchCleanWebsiteContent(company.getWebsite());
            if (fetched.isPresent()) {
                siteContent = fetched.get();
            }
        }

        // 3. Load prompt template
        AiPromptTemplate template = promptTemplateRepository.findByNameAndActiveTrue("COMPANY_ANALYSIS_V1")
                .orElseGet(() -> AiPromptTemplate.builder()
                        .systemPrompt("Tu es un expert en intelligence commerciale B2B au Sénégal. Analyse et retourne du JSON strict avec summary, industry, painPoints, opportunities, recommendedApproach, confidence.")
                        .userPromptTemplate("Entreprise: {companyName}\nSite web: {website}\nContenu:\n{content}")
                        .build());

        String userPrompt = template.getUserPromptTemplate()
                .replace("{companyName}", company.getName())
                .replace("{website}", company.getWebsite() != null ? company.getWebsite() : "Non renseigné")
                .replace("{content}", siteContent);

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(template.getSystemPrompt())
                .userPrompt(userPrompt)
                .responseFormatJson(true)
                .build();

        // 4. Generate AI analysis
        AiResponse aiResponse = aiProvider.generate(aiRequest);

        // 5. Record Usage
        quotaService.recordUsage(
                organizationId, userId, aiProvider.getProviderName(), aiResponse.getModel(),
                "COMPANY_ANALYSIS", aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getDurationMs()
        );

        // 6. Parse structured result
        CompanyAnalysisResult result;
        try {
            result = objectMapper.readValue(aiResponse.getContent(), CompanyAnalysisResult.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON AI response: {}. Falling back to default structured result.", aiResponse.getContent());
            result = CompanyAnalysisResult.builder()
                    .summary(aiResponse.getContent())
                    .industry(company.getIndustry() != null ? company.getIndustry() : "B2B")
                    .painPoints(Collections.singletonList("Optimisation des processus commerciaux"))
                    .opportunities(Collections.singletonList("Digitalisation de la prospection"))
                    .recommendedApproach("Approche directe par WhatsApp ou Email personnalisé.")
                    .confidence(0.85)
                    .build();
        }

        // 7. Update company entity
        company.setAiSummary(result.getSummary());
        try {
            company.setAiPainPoints(objectMapper.writeValueAsString(result.getPainPoints()));
        } catch (Exception ignored) {
        }
        company.setAiAnalyzedAt(Instant.now());
        companyRepository.save(company);

        auditService.logSync("COMPANY_AI_ANALYZED", "Company", companyId.toString(), "Analyzed: " + company.getName());

        return result;
    }
}
