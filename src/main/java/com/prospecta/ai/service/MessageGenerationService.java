package com.prospecta.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.ai.domain.AiPromptTemplate;
import com.prospecta.ai.dto.AiDto.AiRequest;
import com.prospecta.ai.dto.AiDto.AiResponse;
import com.prospecta.ai.dto.AiDto.GenerateMessageRequest;
import com.prospecta.ai.dto.AiDto.GenerateMessageResult;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageGenerationService {

    private final ProspectRepository prospectRepository;
    private final AiPromptTemplateRepository promptTemplateRepository;
    private final AiProvider aiProvider;
    private final QuotaService quotaService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public GenerateMessageResult generateMessage(GenerateMessageRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        UUID userId = TenantContextHolder.getUserPrincipal().map(UserPrincipal::getUserId).orElse(null);

        quotaService.checkQuota(organizationId);

        Prospect prospect = prospectRepository.findByIdAndOrganizationId(request.getProspectId(), organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(request.getProspectId()));

        AiPromptTemplate template = promptTemplateRepository.findByNameAndActiveTrue("MESSAGE_GENERATION_V1")
                .orElseGet(() -> AiPromptTemplate.builder()
                        .systemPrompt("Tu es un copywriter commercial B2B spécialisé au Sénégal. Génère un message personnalisé en JSON strict avec subject, body, channel, callToAction.")
                        .userPromptTemplate("Canal: {channel}\nDestinataire: {prospectName} ({jobTitle} chez {companyName})\nOffre: {offerDescription}\nObjectif: {goal}\nContexte: {context}")
                        .build());

        String userPrompt = template.getUserPromptTemplate()
                .replace("{channel}", request.getChannel())
                .replace("{prospectName}", prospect.getFullName() != null ? prospect.getFullName() : "Responsable")
                .replace("{jobTitle}", prospect.getJobTitle() != null ? prospect.getJobTitle() : "")
                .replace("{companyName}", prospect.getCompanyName() != null ? prospect.getCompanyName() : "votre entreprise")
                .replace("{offerDescription}", request.getOfferDescription())
                .replace("{goal}", request.getGoal() != null ? request.getGoal() : "Obtenir un rendez-vous téléphonique")
                .replace("{context}", prospect.getCity() != null ? "Localisé à " + prospect.getCity() : "Sénégal");

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(template.getSystemPrompt())
                .userPrompt(userPrompt)
                .responseFormatJson(true)
                .build();

        AiResponse aiResponse = aiProvider.generate(aiRequest);

        quotaService.recordUsage(
                organizationId, userId, aiProvider.getProviderName(), aiResponse.getModel(),
                "MESSAGE_GENERATION", aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getDurationMs()
        );

        GenerateMessageResult result;
        try {
            result = objectMapper.readValue(aiResponse.getContent(), GenerateMessageResult.class);
            result.setChannel(request.getChannel().toUpperCase());
        } catch (Exception e) {
            log.warn("Failed to parse JSON message output, constructing fallback result.");
            result = GenerateMessageResult.builder()
                    .channel(request.getChannel().toUpperCase())
                    .subject("Opportunité de collaboration - " + prospect.getCompanyName())
                    .body(aiResponse.getContent())
                    .callToAction("Seriez-vous disponible pour un court échange téléphonique ?")
                    .build();
        }

        auditService.logSync("MESSAGE_GENERATED_AI", "Prospect", prospect.getId().toString(), "Channel: " + request.getChannel());

        return result;
    }
}
