package com.prospecta.ai.service;

import com.prospecta.ai.domain.AiPromptTemplate;
import com.prospecta.ai.dto.AiDto.AiRequest;
import com.prospecta.ai.dto.AiDto.AiResponse;
import com.prospecta.ai.dto.AiDto.ProspectSummaryResult;
import com.prospecta.ai.provider.AiProvider;
import com.prospecta.ai.repository.AiPromptTemplateRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProspectSummaryService {

    private final ProspectRepository prospectRepository;
    private final AiPromptTemplateRepository promptTemplateRepository;
    private final AiProvider aiProvider;
    private final QuotaService quotaService;
    private final AuditService auditService;

    @Transactional
    public ProspectSummaryResult summarizeProspect(UUID prospectId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        UUID userId = TenantContextHolder.getUserPrincipal().map(UserPrincipal::getUserId).orElse(null);

        quotaService.checkQuota(organizationId);

        Prospect prospect = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(prospectId));

        AiPromptTemplate template = promptTemplateRepository.findByNameAndActiveTrue("PROSPECT_SUMMARY_V1")
                .orElseGet(() -> AiPromptTemplate.builder()
                        .systemPrompt("Tu es un assistant de prospection commerciale B2B. Résume les points clés de ce prospect pour le commercial.")
                        .userPromptTemplate("Prospect: {fullName}\nPoste: {jobTitle}\nEntreprise: {companyName}\nLocalisation: {city}, {country}")
                        .build());

        String userPrompt = template.getUserPromptTemplate()
                .replace("{fullName}", prospect.getFullName() != null ? prospect.getFullName() : "Prospect")
                .replace("{jobTitle}", prospect.getJobTitle() != null ? prospect.getJobTitle() : "Non précisé")
                .replace("{companyName}", prospect.getCompanyName() != null ? prospect.getCompanyName() : "Non précisée")
                .replace("{industry}", prospect.getIndustry() != null ? prospect.getIndustry() : "Général")
                .replace("{city}", prospect.getCity() != null ? prospect.getCity() : "Dakar")
                .replace("{country}", prospect.getCountry() != null ? prospect.getCountry() : "SN")
                .replace("{notes}", prospect.getLeadScoreReasons() != null ? prospect.getLeadScoreReasons() : "");

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(template.getSystemPrompt())
                .userPrompt(userPrompt)
                .responseFormatJson(false)
                .build();

        AiResponse aiResponse = aiProvider.generate(aiRequest);

        quotaService.recordUsage(
                organizationId, userId, aiProvider.getProviderName(), aiResponse.getModel(),
                "PROSPECT_SUMMARY", aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getDurationMs()
        );

        auditService.logSync("PROSPECT_AI_SUMMARIZED", "Prospect", prospectId.toString(), "Summarized: " + prospect.getFullName());

        return ProspectSummaryResult.builder()
                .summary(aiResponse.getContent())
                .keyStrengths(List.of(
                        prospect.getCity() != null ? "Localisation: " + prospect.getCity() : "Marché sénégalais",
                        prospect.getWhatsappNumber() != null ? "Joignable sur WhatsApp (+221)" : "Email disponible",
                        prospect.getJobTitle() != null ? "Rôle: " + prospect.getJobTitle() : "Profil qualifié"
                ))
                .suggestedAngle("Mettre l'accent sur les gains de productivité et la réactivité WhatsApp locale.")
                .build();
    }
}
