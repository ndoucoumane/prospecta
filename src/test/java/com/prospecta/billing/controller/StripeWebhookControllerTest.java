package com.prospecta.billing.controller;

import com.prospecta.billing.service.StripeWebhookHandler;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StripeWebhookHandler stripeWebhookHandler;

    @InjectMocks
    private StripeWebhookController stripeWebhookController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(stripeWebhookController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/stripe - should pass payload and signature to handler and return 200")
    void shouldHandleWebhook() throws Exception {
        String payload = "{\"id\":\"evt_123\",\"type\":\"checkout.session.completed\"}";
        String signature = "t=123456,v1=abcdef";

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .header("Stripe-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));

        verify(stripeWebhookHandler).processWebhook(eq(payload), eq(signature));
    }
}
