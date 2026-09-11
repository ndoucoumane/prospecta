package com.prospecta.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.billing.domain.SubscriptionStatus;
import com.prospecta.billing.dto.BillingPlanDto.*;
import com.prospecta.billing.service.BillingService;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BillingService billingService;

    @InjectMocks
    private BillingController billingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(billingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/billing/plans - should return list of plans")
    void shouldReturnPlans() throws Exception {
        BillingPlanResponse plan = BillingPlanResponse.builder()
                .id(OrganizationPlan.STARTER)
                .name("Plan Starter")
                .price(29000L)
                .currency("XOF")
                .billingPeriod("MONTHLY")
                .aiQuota(1000)
                .isCurrent(false)
                .build();

        when(billingService.getAvailablePlans()).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/v1/billing/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("STARTER"))
                .andExpect(jsonPath("$.data[0].price").value(29000))
                .andExpect(jsonPath("$.data[0].currency").value("XOF"));
    }

    @Test
    @DisplayName("GET /api/v1/billing/subscription - should return current subscription")
    void shouldReturnSubscription() throws Exception {
        UUID orgId = UUID.randomUUID();
        OrganizationSubscriptionResponse sub = OrganizationSubscriptionResponse.builder()
                .organizationId(orgId)
                .organizationName("Prospecta Test Org")
                .plan(OrganizationPlan.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .monthlyAiQuota(1000)
                .build();

        when(billingService.getCurrentSubscription()).thenReturn(sub);

        mockMvc.perform(get("/api/v1/billing/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationId").value(orgId.toString()))
                .andExpect(jsonPath("$.data.plan").value("STARTER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.monthlyAiQuota").value(1000));
    }

    @Test
    @DisplayName("POST /api/v1/billing/checkout - should initiate checkout session and return 201")
    void shouldCreateCheckoutSession() throws Exception {
        CreateCheckoutRequest request = CreateCheckoutRequest.builder()
                .plan(OrganizationPlan.BUSINESS)
                .successUrl("https://app.prospecta.sn/billing/success")
                .cancelUrl("https://app.prospecta.sn/billing/cancel")
                .build();

        CheckoutResponse response = CheckoutResponse.builder()
                .checkoutUrl("https://checkout.stripe.com/pay/cs_test_123")
                .sessionId("cs_test_123")
                .build();

        when(billingService.createCheckoutSession(any(CreateCheckoutRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/billing/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_123"))
                .andExpect(jsonPath("$.data.sessionId").value("cs_test_123"));
    }

    @Test
    @DisplayName("POST /api/v1/billing/portal - should return billing portal URL")
    void shouldCreateCustomerPortalSession() throws Exception {
        CustomerPortalRequest request = CustomerPortalRequest.builder()
                .returnUrl("https://app.prospecta.sn/billing")
                .build();

        CustomerPortalResponse response = CustomerPortalResponse.builder()
                .portalUrl("https://billing.stripe.com/p/session_test_456")
                .build();

        when(billingService.createCustomerPortalSession(any(CustomerPortalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/billing/portal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portalUrl").value("https://billing.stripe.com/p/session_test_456"));
    }
}
