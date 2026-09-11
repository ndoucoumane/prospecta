package com.prospecta.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.billing.domain.SubscriptionStatus;
import com.prospecta.messaging.domain.ExternalEvent;
import com.prospecta.messaging.repository.ExternalEventRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.billing.exception.BillingException;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.shared.exception.BusinessException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookHandler {

    private final OrganizationRepository organizationRepository;
    private final ExternalEventRepository externalEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${prospecta.billing.stripe.webhook-secret:whsec_placeholder}")
    private String webhookSecret;

    private boolean isMockMode() {
        return webhookSecret == null || webhookSecret.isBlank() || webhookSecret.contains("placeholder");
    }

    @Transactional
    public void processWebhook(String rawPayload, String signatureHeader) {
        // 1. Validation de la signature cryptographique HMAC (si secret configuré)
        if (!isMockMode()) {
            try {
                Webhook.Signature.verifyHeader(rawPayload, signatureHeader, webhookSecret, 300);
            } catch (SignatureVerificationException e) {
                log.warn("Rejected Stripe webhook: Invalid signature header. Reason: {}", e.getMessage());
                throw new BillingException("INVALID_STRIPE_SIGNATURE", "Signature de webhook Stripe invalide", HttpStatus.FORBIDDEN);
            }
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.path("id").asText();
            String eventType = root.path("type").asText();
            JsonNode dataNode = root.path("data").path("object");

            log.info("Received Stripe webhook event [{}] of type [{}]", eventId, eventType);

            // 2. Vérification d'idempotence (évite de traiter deux fois le même événement)
            if (externalEventRepository.existsByProviderAndExternalEventId("STRIPE", eventId)) {
                log.info("Stripe event [{}] already processed. Skipping.", eventId);
                return;
            }

            // 3. Traitement selon le type d'événement
            switch (eventType) {
                case "checkout.session.completed" -> handleCheckoutSessionCompleted(dataNode);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(dataNode);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(dataNode);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(dataNode);
                default -> log.debug("Unhandled Stripe event type: {}", eventType);
            }

            // 4. Enregistrement de l'événement dans le journal d'événements externes
            ExternalEvent externalEvent = ExternalEvent.builder()
                    .provider("STRIPE")
                    .externalEventId(eventId)
                    .eventType(eventType)
                    .payload(rawPayload)
                    .processed(true)
                    .processedAt(Instant.now())
                    .build();
            externalEventRepository.save(externalEvent);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse or process Stripe webhook: {}", e.getMessage(), e);
            throw new BillingException("WEBHOOK_PROCESSING_FAILED", "Erreur lors du traitement du webhook Stripe: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void handleCheckoutSessionCompleted(JsonNode sessionNode) {
        String clientReferenceId = sessionNode.path("client_reference_id").asText();
        String customerId = sessionNode.path("customer").asText();
        String subscriptionId = sessionNode.path("subscription").asText();
        String targetPlanStr = sessionNode.path("metadata").path("targetPlan").asText("STARTER");

        UUID organizationId = null;
        if (clientReferenceId != null && !clientReferenceId.isBlank()) {
            try {
                organizationId = UUID.fromString(clientReferenceId);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (organizationId == null) {
            String orgIdMeta = sessionNode.path("metadata").path("organizationId").asText();
            if (orgIdMeta != null && !orgIdMeta.isBlank()) {
                try {
                    organizationId = UUID.fromString(orgIdMeta);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (organizationId == null) {
            log.warn("Could not find organizationId in Stripe session metadata");
            return;
        }

        Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
        if (orgOpt.isEmpty()) {
            log.warn("Organization [{}] not found for completed checkout session", organizationId);
            return;
        }

        Organization org = orgOpt.get();
        OrganizationPlan targetPlan;
        try {
            targetPlan = OrganizationPlan.valueOf(targetPlanStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            targetPlan = OrganizationPlan.STARTER;
        }

        org.setPlan(targetPlan);
        org.setStripeCustomerId(customerId);
        org.setStripeSubscriptionId(subscriptionId);
        org.setSubscriptionStatus(SubscriptionStatus.ACTIVE);

        organizationRepository.save(org);
        log.info("Successfully activated [{}] plan for organization [{}]", targetPlan, org.getId());
    }

    private void handleSubscriptionUpdated(JsonNode subNode) {
        String subId = subNode.path("id").asText();
        String statusStr = subNode.path("status").asText("active");
        long currentPeriodEndEpoch = subNode.path("current_period_end").asLong(0);

        Optional<Organization> orgOpt = organizationRepository.findByStripeSubscriptionId(subId);
        if (orgOpt.isEmpty()) {
            String customerId = subNode.path("customer").asText();
            orgOpt = organizationRepository.findByStripeCustomerId(customerId);
        }

        if (orgOpt.isPresent()) {
            Organization org = orgOpt.get();
            SubscriptionStatus status = switch (statusStr.toLowerCase()) {
                case "active" -> SubscriptionStatus.ACTIVE;
                case "trialing" -> SubscriptionStatus.TRIALING;
                case "past_due" -> SubscriptionStatus.PAST_DUE;
                case "canceled" -> SubscriptionStatus.CANCELED;
                case "unpaid" -> SubscriptionStatus.UNPAID;
                default -> SubscriptionStatus.INCOMPLETE;
            };

            org.setSubscriptionStatus(status);
            if (currentPeriodEndEpoch > 0) {
                org.setCurrentPeriodEnd(Instant.ofEpochSecond(currentPeriodEndEpoch));
            }
            organizationRepository.save(org);
            log.info("Updated subscription status to [{}] for organization [{}]", status, org.getId());
        }
    }

    private void handleSubscriptionDeleted(JsonNode subNode) {
        String subId = subNode.path("id").asText();
        Optional<Organization> orgOpt = organizationRepository.findByStripeSubscriptionId(subId);

        if (orgOpt.isPresent()) {
            Organization org = orgOpt.get();
            org.setPlan(OrganizationPlan.FREE);
            org.setSubscriptionStatus(SubscriptionStatus.CANCELED);
            organizationRepository.save(org);
            log.info("Subscription canceled. Reverted organization [{}] to FREE plan", org.getId());
        }
    }

    private void handleInvoicePaymentFailed(JsonNode invoiceNode) {
        String customerId = invoiceNode.path("customer").asText();
        String subId = invoiceNode.path("subscription").asText();

        Optional<Organization> orgOpt = organizationRepository.findByStripeSubscriptionId(subId);
        if (orgOpt.isEmpty()) {
            orgOpt = organizationRepository.findByStripeCustomerId(customerId);
        }

        if (orgOpt.isPresent()) {
            Organization org = orgOpt.get();
            org.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
            organizationRepository.save(org);
            log.warn("Invoice payment failed for organization [{}]. Marked status as PAST_DUE", org.getId());
        }
    }
}
