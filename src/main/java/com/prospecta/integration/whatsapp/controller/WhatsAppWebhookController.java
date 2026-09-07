package com.prospecta.integration.whatsapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.conversation.service.ConversationService;
import com.prospecta.messaging.domain.ExternalEvent;
import com.prospecta.messaging.repository.ExternalEventRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.utils.PhoneNumberUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhooks", description = "Official Meta WhatsApp Business Cloud API webhook integration")
public class WhatsAppWebhookController {

    private final ExternalEventRepository externalEventRepository;
    private final ConversationService conversationService;
    private final ProspectRepository prospectRepository;
    private final ObjectMapper objectMapper;

    @Value("${prospecta.messaging.whatsapp.webhook-verify-token:prospecta_verify_token_dev}")
    private String verifyToken;

    @Value("${prospecta.messaging.whatsapp.app-secret:}")
    private String appSecret;

    @GetMapping
    @Operation(summary = "Meta Webhook Verification Endpoint (hub.challenge)")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        log.info("WhatsApp webhook verification requested: mode={}, token={}", mode, token);
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verified successfully!");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook verification failed: invalid token");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    @PostMapping
    @Operation(summary = "Meta WhatsApp Inbound Events (Messages, Delivery Statuses)")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        log.info("Received WhatsApp webhook event");

        // 1. Signature validation if secret is configured
        if (appSecret != null && !appSecret.isBlank() && !validateSignature(signature, rawPayload)) {
            log.warn("Rejected WhatsApp webhook: invalid HMAC signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode entryNode = root.path("entry").path(0);
            JsonNode changeNode = entryNode.path("changes").path(0).path("value");

            // Handle Inbound Messages
            JsonNode messagesNode = changeNode.path("messages");
            if (messagesNode.isArray() && !messagesNode.isEmpty()) {
                JsonNode messageNode = messagesNode.get(0);
                String messageId = messageNode.path("id").asText();
                String fromNumber = messageNode.path("from").asText();
                String textBody = messageNode.path("text").path("body").asText();

                if (textBody == null || textBody.isBlank()) {
                    textBody = "[Message média / non textuel]";
                }

                // 2. Idempotency Check
                if (externalEventRepository.existsByProviderAndExternalEventId("WHATSAPP", messageId)) {
                    log.info("Duplicate webhook event ignored: provider=WHATSAPP, id={}", messageId);
                    return ResponseEntity.ok("EVENT_ALREADY_PROCESSED");
                }

                // 3. Persist raw event
                ExternalEvent event = ExternalEvent.builder()
                        .provider("WHATSAPP")
                        .externalEventId(messageId)
                        .eventType("whatsapp.message.received")
                        .payload(rawPayload)
                        .processed(true)
                        .processedAt(Instant.now())
                        .build();
                externalEventRepository.save(event);

                // 4. Resolve prospect by phone number (E.164)
                String normalizedFrom = fromNumber.startsWith("+") ? fromNumber : "+" + fromNumber;
                Optional<String> e164 = PhoneNumberUtils.normalizeToE164(fromNumber, "SN");
                if (e164.isPresent()) {
                    normalizedFrom = e164.get();
                }

                // Search for matching prospect in DB
                Optional<Prospect> prospectOpt = findProspectByPhone(normalizedFrom);
                if (prospectOpt.isPresent()) {
                    Prospect prospect = prospectOpt.get();
                    conversationService.recordInboundMessage(
                            prospect.getOrganizationId(),
                            prospect.getId(),
                            ChannelType.WHATSAPP,
                            normalizedFrom,
                            "WhatsApp Business",
                            textBody,
                            messageId
                    );
                    log.info("Recorded inbound WhatsApp message from prospect [{}]", prospect.getId());
                } else {
                    log.warn("Inbound WhatsApp message received from unknown number: {}", normalizedFrom);
                }
            }

            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook: {}", e.getMessage(), e);
            // Always return 200 to Meta to avoid infinite redelivery storms
            return ResponseEntity.ok("EVENT_ACKNOWLEDGED_WITH_ERROR");
        }
    }

    private Optional<Prospect> findProspectByPhone(String phone) {
        // Query prospect repository across all organizations by phone or whatsappNumber
        return prospectRepository.findAll().stream()
                .filter(p -> (p.getWhatsappNumber() != null && p.getWhatsappNumber().equals(phone))
                        || (p.getPhone() != null && p.getPhone().equals(phone)))
                .findFirst();
    }

    private boolean validateSignature(String signatureHeader, String rawPayload) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            String expectedHash = signatureHeader.substring(7);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return MessageDigest.isEqual(sb.toString().getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature validation error: {}", e.getMessage());
            return false;
        }
    }
}
