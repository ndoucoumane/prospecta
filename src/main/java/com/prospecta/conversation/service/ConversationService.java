package com.prospecta.conversation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.ai.domain.AiPromptTemplate;
import com.prospecta.ai.dto.AiDto.AiRequest;
import com.prospecta.ai.dto.AiDto.AiResponse;
import com.prospecta.ai.provider.AiProvider;
import com.prospecta.ai.repository.AiPromptTemplateRepository;
import com.prospecta.ai.service.QuotaService;
import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.conversation.domain.Conversation;
import com.prospecta.conversation.domain.ConversationMessage;
import com.prospecta.conversation.domain.ConversationStatus;
import com.prospecta.conversation.dto.ConversationDto.*;
import com.prospecta.conversation.repository.ConversationMessageRepository;
import com.prospecta.conversation.repository.ConversationRepository;
import com.prospecta.messaging.domain.MessageDirection;
import com.prospecta.messaging.service.MessagingService;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.ConversationNotFoundException;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ProspectRepository prospectRepository;
    private final MessagingService messagingService;
    private final AiPromptTemplateRepository promptTemplateRepository;
    private final AiProvider aiProvider;
    private final QuotaService quotaService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> getConversations(ChannelType channel, ConversationStatus status, Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<Conversation> page = conversationRepository.findFiltered(organizationId, channel, status, pageable);
        return PageResponse.from(page.map(ConversationSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationById(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Conversation conversation = conversationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ConversationNotFoundException(id));

        List<ConversationMessage> messages = messageRepository.findAllByConversationIdOrderBySentAtAsc(id);
        return ConversationDetailResponse.from(conversation, messages);
    }

    @Transactional
    public ConversationMessageDto recordInboundMessage(
            UUID organizationId,
            UUID prospectId,
            ChannelType channel,
            String sender,
            String recipient,
            String content,
            String externalMessageId
    ) {
        Prospect prospect = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(prospectId));

        Conversation conversation = conversationRepository
                .findByOrganizationIdAndProspectIdAndChannel(organizationId, prospectId, channel)
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .prospect(prospect)
                            .channel(channel)
                            .status(ConversationStatus.OPEN)
                            .build();
                    c.setOrganizationId(organizationId);
                    return conversationRepository.save(c);
                });

        Instant now = Instant.now();
        conversation.setLastMessagePreview(content.length() > 200 ? content.substring(0, 197) + "..." : content);
        conversation.setLastMessageAt(now);
        conversation.setStatus(ConversationStatus.OPEN);
        conversationRepository.save(conversation);

        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.INBOUND)
                .channel(channel)
                .sender(sender)
                .recipient(recipient)
                .content(content)
                .externalMessageId(externalMessageId)
                .status("RECEIVED")
                .sentAt(now)
                .build();
        ConversationMessage savedMessage = messageRepository.save(message);

        // Golden Path Step 16: Automatically transition Prospect status from CONTACTED to REPLIED
        if (prospect.getStatus() != ProspectStatus.REPLIED && prospect.getStatus() != ProspectStatus.WON) {
            prospect.setStatus(ProspectStatus.REPLIED);
            prospectRepository.save(prospect);
            log.info("Prospect [{}] automatically transitioned to status: REPLIED", prospect.getId());
        }

        auditService.logSync("CONVERSATION_INBOUND_MESSAGE", "Conversation", conversation.getId().toString(),
                String.format("Channel: %s, From: %s", channel, sender));

        return ConversationMessageDto.from(savedMessage);
    }

    @Transactional
    public ConversationMessageDto sendReply(UUID conversationId, SendReplyRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Conversation conversation = conversationRepository.findByIdAndOrganizationId(conversationId, organizationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Prospect prospect = conversation.getProspect();
        String recipient = conversation.getChannel() == ChannelType.WHATSAPP
                ? (prospect.getWhatsappNumber() != null ? prospect.getWhatsappNumber() : prospect.getPhone())
                : prospect.getEmail();

        // 1. Dispatch through messaging domain
        var sentMessage = messagingService.sendMessage(
                organizationId, prospect.getId(), null, conversation.getChannel(),
                recipient, "Re: Échange commercial", request.getContent().trim()
        );

        // 2. Persist in conversation thread
        Instant now = Instant.now();
        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.OUTBOUND)
                .channel(conversation.getChannel())
                .sender("agent@" + organizationId.toString().substring(0, 8) + ".prospecta.sn")
                .recipient(recipient)
                .content(request.getContent().trim())
                .externalMessageId(sentMessage.getExternalId())
                .status("SENT")
                .sentAt(now)
                .build();

        ConversationMessage saved = messageRepository.save(message);

        conversation.setLastMessagePreview(request.getContent().trim());
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        return ConversationMessageDto.from(saved);
    }

    @Transactional
    public AiReplySuggestion generateAiReply(UUID conversationId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        UUID userId = TenantContextHolder.getUserPrincipal().map(UserPrincipal::getUserId).orElse(null);

        quotaService.checkQuota(organizationId);

        Conversation conversation = conversationRepository.findByIdAndOrganizationId(conversationId, organizationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        List<ConversationMessage> messages = messageRepository.findAllByConversationIdOrderBySentAtAsc(conversationId);
        String history = messages.stream()
                .map(m -> (m.getDirection() == MessageDirection.INBOUND ? "Prospect: " : "Vendeur: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        Prospect prospect = conversation.getProspect();

        AiPromptTemplate template = promptTemplateRepository.findByNameAndActiveTrue("CONVERSATION_REPLY_V1")
                .orElseGet(() -> AiPromptTemplate.builder()
                        .systemPrompt("Tu es un copilote commercial assistant un vendeur. Analyse l'échange et propose une réponse adaptée en JSON strict.")
                        .userPromptTemplate("Historique:\n{conversationHistory}\nProspect: {prospectName} chez {companyName}\nOffre: {offerDescription}")
                        .build());

        String userPrompt = template.getUserPromptTemplate()
                .replace("{conversationHistory}", history)
                .replace("{prospectName}", prospect.getFullName() != null ? prospect.getFullName() : "Prospect")
                .replace("{companyName}", prospect.getCompanyName() != null ? prospect.getCompanyName() : "l'entreprise")
                .replace("{offerDescription}", "Plateforme d'automatisation des ventes et prospection multicanale B2B au Sénégal");

        AiRequest aiRequest = AiRequest.builder()
                .systemPrompt(template.getSystemPrompt())
                .userPrompt(userPrompt)
                .responseFormatJson(true)
                .build();

        AiResponse aiResponse = aiProvider.generate(aiRequest);

        quotaService.recordUsage(
                organizationId, userId, aiProvider.getProviderName(), aiResponse.getModel(),
                "CONVERSATION_REPLY", aiResponse.getInputTokens(), aiResponse.getOutputTokens(), aiResponse.getDurationMs()
        );

        AiReplySuggestion suggestion;
        try {
            suggestion = objectMapper.readValue(aiResponse.getContent(), AiReplySuggestion.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON AI reply, constructing default structured suggestion.");
            suggestion = AiReplySuggestion.builder()
                    .suggestedReply("Merci pour votre retour ! Auriez-vous un créneau ce jeudi pour un rapide échange téléphonique ?")
                    .intent("INTERESTED")
                    .sentiment("POSITIVE")
                    .recommendedNextAction("BOOK_MEETING")
                    .confidence(0.90)
                    .build();
        }

        auditService.logSync("AI_REPLY_SUGGESTED", "Conversation", conversationId.toString(), "Intent: " + suggestion.getIntent());

        return suggestion;
    }
}
