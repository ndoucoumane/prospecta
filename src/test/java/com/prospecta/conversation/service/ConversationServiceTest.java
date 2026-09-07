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
import com.prospecta.conversation.dto.ConversationDto.AiReplySuggestion;
import com.prospecta.conversation.dto.ConversationDto.ConversationMessageDto;
import com.prospecta.conversation.dto.ConversationDto.SendReplyRequest;
import com.prospecta.conversation.repository.ConversationMessageRepository;
import com.prospecta.conversation.repository.ConversationRepository;
import com.prospecta.messaging.domain.Message;
import com.prospecta.messaging.domain.MessageDirection;
import com.prospecta.messaging.service.MessagingService;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private MessagingService messagingService;

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
    private ConversationService conversationService;

    private UUID organizationId;
    private UUID prospectId;
    private UUID conversationId;
    private Prospect prospect;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        prospectId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        TenantContextHolder.setContext(TenantContext.builder()
                .organizationId(organizationId)
                .userPrincipal(UserPrincipal.builder()
                        .userId(UUID.randomUUID())
                        .email("agent@teranga.sn")
                        .organizationId(organizationId)
                        .role(com.prospecta.identity.domain.UserRole.SALES_REP)
                        .permissions(java.util.Set.of())
                        .build())
                .build());

        prospect = Prospect.builder()
                .fullName("Moussa Diop")
                .companyName("Teranga Tech")
                .email("moussa@teranga.sn")
                .phone("+221771234567")
                .whatsappNumber("+221771234567")
                .status(ProspectStatus.CONTACTED)
                .build();
        prospect.setId(prospectId);
        prospect.setOrganizationId(organizationId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Inbound message automatically transitions Prospect status from CONTACTED to REPLIED")
    void recordInboundMessage_autoTransitionsProspectStatusToReplied() {
        Conversation conversation = Conversation.builder()
                .prospect(prospect)
                .channel(ChannelType.WHATSAPP)
                .status(ConversationStatus.OPEN)
                .build();
        conversation.setId(conversationId);
        conversation.setOrganizationId(organizationId);

        when(prospectRepository.findByIdAndOrganizationId(prospectId, organizationId))
                .thenReturn(Optional.of(prospect));
        when(conversationRepository.findByOrganizationIdAndProspectIdAndChannel(organizationId, prospectId, ChannelType.WHATSAPP))
                .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(i -> {
            ConversationMessage msg = i.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });

        ConversationMessageDto result = conversationService.recordInboundMessage(
                organizationId, prospectId, ChannelType.WHATSAPP,
                "+221771234567", "Prospecta WhatsApp", "Bonjour, je suis intéressé par une démo.", "wamid.12345"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Bonjour, je suis intéressé par une démo.");
        assertThat(result.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.REPLIED);

        verify(prospectRepository).save(prospect);
        verify(conversationRepository).save(conversation);
        verify(auditService).logSync(eq("CONVERSATION_INBOUND_MESSAGE"), eq("Conversation"), any(), any());
    }

    @Test
    @DisplayName("Generate AI reply copilot suggestion analyzes thread and returns structured recommendation")
    void generateAiReply_returnsStructuredSuggestion() {
        Conversation conversation = Conversation.builder()
                .prospect(prospect)
                .channel(ChannelType.WHATSAPP)
                .status(ConversationStatus.OPEN)
                .build();
        conversation.setId(conversationId);
        conversation.setOrganizationId(organizationId);

        when(conversationRepository.findByIdAndOrganizationId(conversationId, organizationId))
                .thenReturn(Optional.of(conversation));

        ConversationMessage inboundMsg = ConversationMessage.builder()
                .conversation(conversation)
                .direction(MessageDirection.INBOUND)
                .channel(ChannelType.WHATSAPP)
                .content("Bonjour, pouvez-vous m'envoyer votre plaquette et vos tarifs ?")
                .sentAt(Instant.now())
                .build();

        when(messageRepository.findAllByConversationIdOrderBySentAtAsc(conversationId))
                .thenReturn(List.of(inboundMsg));

        when(promptTemplateRepository.findByNameAndActiveTrue("CONVERSATION_REPLY_V1"))
                .thenReturn(Optional.of(AiPromptTemplate.builder()
                        .systemPrompt("Copilote commercial.")
                        .userPromptTemplate("Historique: {conversationHistory} - Prospect: {prospectName}")
                        .build()));

        String aiJson = """
                {
                  "suggestedReply": "Bonjour Moussa, ravi de votre retour ! Je vous joins notre présentation. Auriez-vous 15 min demain matin pour un échange rapide ?",
                  "intent": "INFORMATION_REQUEST",
                  "sentiment": "POSITIVE",
                  "recommendedNextAction": "SHARE_DECK_AND_BOOK_MEETING",
                  "confidence": 0.95
                }
                """;

        when(aiProvider.getProviderName()).thenReturn("openai");
        when(aiProvider.generate(any(AiRequest.class))).thenReturn(AiResponse.builder()
                .content(aiJson)
                .model("gpt-4o-mini")
                .inputTokens(120)
                .outputTokens(65)
                .durationMs(450)
                .build());

        AiReplySuggestion suggestion = conversationService.generateAiReply(conversationId);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.getIntent()).isEqualTo("INFORMATION_REQUEST");
        assertThat(suggestion.getSentiment()).isEqualTo("POSITIVE");
        assertThat(suggestion.getConfidence()).isEqualTo(0.95);
        assertThat(suggestion.getSuggestedReply()).contains("Bonjour Moussa");

        verify(quotaService).checkQuota(organizationId);
        verify(quotaService).recordUsage(eq(organizationId), any(), eq("openai"), eq("gpt-4o-mini"), eq("CONVERSATION_REPLY"), eq(120), eq(65), eq(450L));
    }

    @Test
    @DisplayName("Send reply sends message via messaging service and records outbound message in thread")
    void sendReply_sendsViaMessagingAndAppendsToThread() {
        Conversation conversation = Conversation.builder()
                .prospect(prospect)
                .channel(ChannelType.WHATSAPP)
                .status(ConversationStatus.OPEN)
                .build();
        conversation.setId(conversationId);
        conversation.setOrganizationId(organizationId);

        when(conversationRepository.findByIdAndOrganizationId(conversationId, organizationId))
                .thenReturn(Optional.of(conversation));

        Message sentMsg = Message.builder()
                .externalId("wamid.OUTBOUND_123")
                .recipient("+221771234567")
                .content("Parfait, appelons-nous jeudi à 10h.")
                .build();

        when(messagingService.sendMessage(eq(organizationId), eq(prospectId), isNull(), eq(ChannelType.WHATSAPP),
                eq("+221771234567"), any(), eq("Parfait, appelons-nous jeudi à 10h.")))
                .thenReturn(sentMsg);

        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(i -> {
            ConversationMessage msg = i.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });

        ConversationMessageDto replyDto = conversationService.sendReply(
                conversationId,
                SendReplyRequest.builder().content("Parfait, appelons-nous jeudi à 10h.").build()
        );

        assertThat(replyDto).isNotNull();
        assertThat(replyDto.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(replyDto.getContent()).isEqualTo("Parfait, appelons-nous jeudi à 10h.");

        verify(messageRepository).save(argThat(msg ->
                "wamid.OUTBOUND_123".equals(msg.getExternalMessageId()) &&
                "+221771234567".equals(msg.getRecipient())));
        verify(conversationRepository).save(conversation);
    }
}
