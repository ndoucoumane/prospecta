package com.prospecta.billing.dto;

import com.prospecta.billing.domain.SubscriptionStatus;
import com.prospecta.organization.domain.OrganizationPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BillingPlanDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingPlanResponse {
        private OrganizationPlan id;
        private String name;
        private String description;
        private long price;
        private String currency;
        private String billingPeriod;
        private long aiQuota;
        private List<String> features;
        private boolean isCurrent;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCheckoutRequest {
        @NotNull(message = "Plan is required (STARTER, BUSINESS)")
        private OrganizationPlan plan;

        @NotBlank(message = "successUrl is required")
        private String successUrl;

        @NotBlank(message = "cancelUrl is required")
        private String cancelUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutResponse {
        private String checkoutUrl;
        private String sessionId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerPortalRequest {
        @NotBlank(message = "returnUrl is required")
        private String returnUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerPortalResponse {
        private String portalUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationSubscriptionResponse {
        private UUID organizationId;
        private String organizationName;
        private OrganizationPlan plan;
        private SubscriptionStatus status;
        private String stripeCustomerId;
        private String stripeSubscriptionId;
        private Instant currentPeriodEnd;
        private long monthlyAiQuota;
    }
}
