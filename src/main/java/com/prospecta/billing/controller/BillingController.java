package com.prospecta.billing.controller;

import com.prospecta.billing.dto.BillingPlanDto.*;
import com.prospecta.billing.service.BillingService;
import com.prospecta.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing & Subscriptions", description = "SaaS plans, Stripe Checkout subscriptions, and Customer Portal")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    @Operation(summary = "Get available subscription plans with quotas and features")
    public ResponseEntity<ApiResponse<List<BillingPlanResponse>>> getAvailablePlans() {
        List<BillingPlanResponse> plans = billingService.getAvailablePlans();
        return ResponseEntity.ok(ApiResponse.of(plans));
    }

    @GetMapping("/subscription")
    @Operation(summary = "Get active subscription details for the current organization")
    public ResponseEntity<ApiResponse<OrganizationSubscriptionResponse>> getCurrentSubscription() {
        OrganizationSubscriptionResponse response = billingService.getCurrentSubscription();
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Initiate Stripe Checkout session for plan upgrade (admin only)")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutRequest request
    ) {
        CheckoutResponse response = billingService.createCheckoutSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/portal")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Access Stripe Customer Portal for managing payment methods and invoices (admin only)")
    public ResponseEntity<ApiResponse<CustomerPortalResponse>> createCustomerPortalSession(
            @Valid @RequestBody CustomerPortalRequest request
    ) {
        CustomerPortalResponse response = billingService.createCustomerPortalSession(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
