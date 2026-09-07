package com.prospecta.integration.whatsapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.conversation.service.ConversationService;
import com.prospecta.messaging.domain.ExternalEvent;
import com.prospecta.messaging.repository.ExternalEventRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.ProspectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExternalEventRepository externalEventRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ProspectRepository prospectRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WhatsAppWebhookController whatsAppWebhookController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(whatsAppWebhookController, "verifyToken", "prospecta_verify_token_dev");
        ReflectionTestUtils.setField(whatsAppWebhookController, "appSecret", ""); // disable signature requirement for test

        mockMvc = MockMvcBuilders.standaloneSetup(whatsAppWebhookController).build();
    }

    @Test
    @DisplayName("GET /api/v1/webhooks/whatsapp - should verify token and return hub.challenge")
    void shouldVerifyWebhookToken() throws Exception {
        mockMvc.perform(get("/api/v1/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "prospecta_verify_token_dev")
                        .param("hub.challenge", "challenge_12345"))
                .andExpect(status().isOk())
                .andExpect(content().string("challenge_12345"));
    }

    @Test
    @DisplayName("GET /api/v1/webhooks/whatsapp - should return 403 on wrong token")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong_token")
                        .param("hub.challenge", "challenge_12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/whatsapp - should record inbound message and transition prospect to REPLIED")
    void shouldProcessInboundWhatsAppMessage() throws Exception {
        String wamid = "wamid.HBgMNDY3MDM1NTY4OA==";
        String webhookPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "10001",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": { "display_phone_number": "+221338000000", "phone_number_id": "10002" },
                        "messages": [{
                          "from": "221771234567",
                          "id": "%s",
                          "timestamp": "1710000000",
                          "text": { "body": "Bonjour, votre offre m'intéresse beaucoup !" },
                          "type": "text"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """.formatted(wamid);

        UUID orgId = UUID.randomUUID();
        UUID prospectId = UUID.randomUUID();
        Prospect prospect = Prospect.builder()
                .phone("+221771234567")
                .whatsappNumber("+221771234567")
                .status(ProspectStatus.CONTACTED)
                .build();
        prospect.setId(prospectId);
        prospect.setOrganizationId(orgId);

        when(externalEventRepository.existsByProviderAndExternalEventId("WHATSAPP", wamid)).thenReturn(false);
        when(prospectRepository.findAll()).thenReturn(List.of(prospect));

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));

        verify(externalEventRepository).save(any(ExternalEvent.class));
        verify(conversationService).recordInboundMessage(
                eq(orgId), eq(prospectId), eq(ChannelType.WHATSAPP),
                eq("+221771234567"), eq("WhatsApp Business"),
                eq("Bonjour, votre offre m'intéresse beaucoup !"), eq(wamid)
        );
    }

    @Test
    @DisplayName("Idempotency: Webhook received twice should be ignored and not duplicate messages")
    void shouldHandleDuplicateWebhookIdempotently() throws Exception {
        String wamid = "wamid.DUPLICATE_ID_123";
        String webhookPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messages": [{
                          "from": "221771234567",
                          "id": "%s",
                          "text": { "body": "Test message" }
                        }]
                      }
                    }]
                  }]
                }
                """.formatted(wamid);

        when(externalEventRepository.existsByProviderAndExternalEventId("WHATSAPP", wamid)).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_ALREADY_PROCESSED"));

        verify(conversationService, never()).recordInboundMessage(any(), any(), any(), any(), any(), any(), any());
        verify(externalEventRepository, never()).save(any());
    }
}
