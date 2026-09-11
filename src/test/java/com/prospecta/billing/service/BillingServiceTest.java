package com.prospecta.billing.service;

import com.prospecta.billing.domain.SubscriptionStatus;
import com.prospecta.billing.dto.BillingPlanDto.*;
import com.prospecta.billing.exception.BillingException;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.repository.OrganizationRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BillingService billingService;

    private UUID organizationId;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = Organization.builder()
                .name("Agence Digitale Dakar")
                .email("contact@agence.sn")
                .plan(OrganizationPlan.FREE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();
        organization.setId(organizationId);

        UserPrincipal principal = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .organizationId(organizationId)
                .email("admin@agence.sn")
                .permissions(Collections.emptySet())
                .build();
        TenantContextHolder.setContext(TenantContext.of(organizationId, principal, "trace-billing-test"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should return available plans with FREE marked as current")
    void shouldReturnAvailablePlans() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        List<BillingPlanResponse> plans = billingService.getAvailablePlans();

        assertThat(plans).hasSize(3);

        BillingPlanResponse freePlan = plans.get(0);
        assertThat(freePlan.getId()).isEqualTo(OrganizationPlan.FREE);
        assertThat(freePlan.getPrice()).isEqualTo(0L);
        assertThat(freePlan.getCurrency()).isEqualTo("XOF");
        assertThat(freePlan.isCurrent()).isTrue();
        assertThat(freePlan.getAiQuota()).isEqualTo(100);

        BillingPlanResponse starterPlan = plans.get(1);
        assertThat(starterPlan.getId()).isEqualTo(OrganizationPlan.STARTER);
        assertThat(starterPlan.getPrice()).isEqualTo(29000L);
        assertThat(starterPlan.isCurrent()).isFalse();
        assertThat(starterPlan.getAiQuota()).isEqualTo(1000);

        BillingPlanResponse businessPlan = plans.get(2);
        assertThat(businessPlan.getId()).isEqualTo(OrganizationPlan.BUSINESS);
        assertThat(businessPlan.getPrice()).isEqualTo(79000L);
        assertThat(businessPlan.isCurrent()).isFalse();
        assertThat(businessPlan.getAiQuota()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should return current subscription details")
    void shouldReturnCurrentSubscription() {
        organization.setPlan(OrganizationPlan.STARTER);
        organization.setStripeCustomerId("cus_test_123");
        organization.setStripeSubscriptionId("sub_test_456");
        organization.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        organization.setCurrentPeriodEnd(Instant.parse("2026-10-01T00:00:00Z"));

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        OrganizationSubscriptionResponse response = billingService.getCurrentSubscription();

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        assertThat(response.getPlan()).isEqualTo(OrganizationPlan.STARTER);
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getStripeCustomerId()).isEqualTo("cus_test_123");
        assertThat(response.getStripeSubscriptionId()).isEqualTo("sub_test_456");
        assertThat(response.getMonthlyAiQuota()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should throw 404 when organization not found for current subscription")
    void shouldThrowWhenOrgNotFoundForSubscription() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.getCurrentSubscription())
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw 400 when attempting to checkout FREE plan")
    void shouldRejectCheckoutForFreePlan() {
        CreateCheckoutRequest request = CreateCheckoutRequest.builder()
                .plan(OrganizationPlan.FREE)
                .successUrl("https://app.prospecta.sn/billing/success")
                .cancelUrl("https://app.prospecta.sn/billing/cancel")
                .build();

        assertThatThrownBy(() -> billingService.createCheckoutSession(request))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should create checkout session successfully for STARTER plan")
    void shouldCreateCheckoutSessionForStarter() {
        CreateCheckoutRequest request = CreateCheckoutRequest.builder()
                .plan(OrganizationPlan.STARTER)
                .successUrl("https://app.prospecta.sn/billing/success?session_id={CHECKOUT_SESSION_ID}")
                .cancelUrl("https://app.prospecta.sn/billing/cancel")
                .build();

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(paymentGateway.createCheckoutSession(eq(organization), eq(OrganizationPlan.STARTER), any(), any()))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_test_abc123");

        CheckoutResponse response = billingService.createCheckoutSession(request);

        assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_abc123");
        verify(organizationRepository).save(organization);
    }

    @Test
    @DisplayName("Should throw 400 when customer portal requested without Stripe customer ID")
    void shouldThrowWhenNoStripeCustomerIdForPortal() {
        organization.setStripeCustomerId(null);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        CustomerPortalRequest request = CustomerPortalRequest.builder()
                .returnUrl("https://app.prospecta.sn/billing")
                .build();

        assertThatThrownBy(() -> billingService.createCustomerPortalSession(request))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should create customer portal session when Stripe customer ID is present")
    void shouldCreateCustomerPortalSession() {
        organization.setStripeCustomerId("cus_abc123");
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(paymentGateway.createCustomerPortalSession(eq("cus_abc123"), eq("https://app.prospecta.sn/billing")))
                .thenReturn("https://billing.stripe.com/p/session_test_xyz");

        CustomerPortalRequest request = CustomerPortalRequest.builder()
                .returnUrl("https://app.prospecta.sn/billing")
                .build();

        CustomerPortalResponse response = billingService.createCustomerPortalSession(request);

        assertThat(response.getPortalUrl()).isEqualTo("https://billing.stripe.com/p/session_test_xyz");
    }
}
