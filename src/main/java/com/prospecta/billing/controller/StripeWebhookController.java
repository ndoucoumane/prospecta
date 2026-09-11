package com.prospecta.billing.controller;

import com.prospecta.billing.service.StripeWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhooks", description = "Asynchronous payment and subscription events from Stripe")
public class StripeWebhookController {

    private final StripeWebhookHandler stripeWebhookHandler;

    @PostMapping
    @Operation(summary = "Handle inbound Stripe webhook events (checkout completed, subscription updated/deleted)")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(name = "Stripe-Signature", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        log.info("Received inbound Stripe webhook notification");
        stripeWebhookHandler.processWebhook(rawPayload, signature);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
