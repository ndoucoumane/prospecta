package com.prospecta.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.billing.domain.SubscriptionStatus;
import com.prospecta.messaging.domain.ExternalEvent;
import com.prospecta.messaging.repository.ExternalEventRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookHandlerTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ExternalEventRepository externalEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StripeWebhookHandler stripeWebhookHandler;

    private UUID organizationId;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = Organization.builder()
                .name("Sahel Tech SAS")
                .email("contact@saheltech.sn")
                .plan(OrganizationPlan.FREE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();
        organization.setId(organizationId);

        // Configure mock mode for testing without real signature check
        ReflectionTestUtils.setField(stripeWebhookHandler, "webhookSecret", "whsec_placeholder");
    }

    @Test
    @DisplayName("Should process checkout.session.completed and upgrade plan to BUSINESS")
    void shouldHandleCheckoutSessionCompleted() {
        String eventId = "evt_test_checkout_123";
        String payload = """
                {
                  "id": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_abc",
                      "client_reference_id": "%s",
                      "customer": "cus_stripe_999",
                      "subscription": "sub_stripe_888",
                      "metadata": {
                        "targetPlan": "BUSINESS"
                      }
                    }
                  }
                }
                """.formatted(eventId, organizationId);

        when(externalEventRepository.existsByProviderAndExternalEventId("STRIPE", eventId)).thenReturn(false);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        stripeWebhookHandler.processWebhook(payload, "dummy_sig");

        assertThat(organization.getPlan()).isEqualTo(OrganizationPlan.BUSINESS);
        assertThat(organization.getStripeCustomerId()).isEqualTo("cus_stripe_999");
        assertThat(organization.getStripeSubscriptionId()).isEqualTo("sub_stripe_888");
        assertThat(organization.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        verify(organizationRepository).save(organization);

        ArgumentCaptor<ExternalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalEvent.class);
        verify(externalEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getExternalEventId()).isEqualTo(eventId);
        assertThat(eventCaptor.getValue().getProvider()).isEqualTo("STRIPE");
        assertThat(eventCaptor.getValue().isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Should skip processing when webhook event was already processed (Idempotency)")
    void shouldSkipAlreadyProcessedEvent() {
        String eventId = "evt_test_duplicate";
        String payload = """
                {
                  "id": "%s",
                  "type": "checkout.session.completed",
                  "data": { "object": {} }
                }
                """.formatted(eventId);

        when(externalEventRepository.existsByProviderAndExternalEventId("STRIPE", eventId)).thenReturn(true);

        stripeWebhookHandler.processWebhook(payload, "dummy_sig");

        verify(organizationRepository, never()).findById(any());
        verify(organizationRepository, never()).save(any());
        verify(externalEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle customer.subscription.updated and update status and period end")
    void shouldHandleSubscriptionUpdated() {
        String subId = "sub_stripe_888";
        organization.setStripeSubscriptionId(subId);

        String eventId = "evt_sub_updated";
        long periodEndEpoch = Instant.now().plusSeconds(86400 * 30).getEpochSecond();
        String payload = """
                {
                  "id": "%s",
                  "type": "customer.subscription.updated",
                  "data": {
                    "object": {
                      "id": "%s",
                      "status": "trialing",
                      "current_period_end": %d
                    }
                  }
                }
                """.formatted(eventId, subId, periodEndEpoch);

        when(externalEventRepository.existsByProviderAndExternalEventId("STRIPE", eventId)).thenReturn(false);
        when(organizationRepository.findByStripeSubscriptionId(subId)).thenReturn(Optional.of(organization));

        stripeWebhookHandler.processWebhook(payload, "dummy_sig");

        assertThat(organization.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(organization.getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(periodEndEpoch));
        verify(organizationRepository).save(organization);
    }

    @Test
    @DisplayName("Should handle customer.subscription.deleted and revert to FREE plan")
    void shouldHandleSubscriptionDeleted() {
        String subId = "sub_stripe_888";
        organization.setPlan(OrganizationPlan.BUSINESS);
        organization.setStripeSubscriptionId(subId);

        String eventId = "evt_sub_deleted";
        String payload = """
                {
                  "id": "%s",
                  "type": "customer.subscription.deleted",
                  "data": {
                    "object": {
                      "id": "%s"
                    }
                  }
                }
                """.formatted(eventId, subId);

        when(externalEventRepository.existsByProviderAndExternalEventId("STRIPE", eventId)).thenReturn(false);
        when(organizationRepository.findByStripeSubscriptionId(subId)).thenReturn(Optional.of(organization));

        stripeWebhookHandler.processWebhook(payload, "dummy_sig");

        assertThat(organization.getPlan()).isEqualTo(OrganizationPlan.FREE);
        assertThat(organization.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(organizationRepository).save(organization);
    }

    @Test
    @DisplayName("Should handle invoice.payment_failed and set status to PAST_DUE")
    void shouldHandleInvoicePaymentFailed() {
        String subId = "sub_stripe_888";
        organization.setPlan(OrganizationPlan.STARTER);
        organization.setStripeSubscriptionId(subId);

        String eventId = "evt_invoice_failed";
        String payload = """
                {
                  "id": "%s",
                  "type": "invoice.payment_failed",
                  "data": {
                    "object": {
                      "subscription": "%s",
                      "customer": "cus_123"
                    }
                  }
                }
                """.formatted(eventId, subId);

        when(externalEventRepository.existsByProviderAndExternalEventId("STRIPE", eventId)).thenReturn(false);
        when(organizationRepository.findByStripeSubscriptionId(subId)).thenReturn(Optional.of(organization));

        stripeWebhookHandler.processWebhook(payload, "dummy_sig");

        assertThat(organization.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        verify(organizationRepository).save(organization);
    }
}
