package com.prospecta.billing.service;

import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.billing.exception.BillingException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class StripePaymentGateway implements PaymentGateway {

    @Value("${prospecta.billing.stripe.api-key:sk_test_placeholder}")
    private String apiKey;

    @Value("${prospecta.billing.plans.starter.price-id:price_starter_test}")
    private String starterPriceId;

    @Value("${prospecta.billing.plans.starter.amount:29000}")
    private long starterAmount;

    @Value("${prospecta.billing.plans.business.price-id:price_business_test}")
    private String businessPriceId;

    @Value("${prospecta.billing.plans.business.amount:79000}")
    private long businessAmount;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
        }
    }

    private boolean isMockMode() {
        return apiKey == null || apiKey.isBlank() || apiKey.contains("placeholder") || apiKey.contains("test_placeholder");
    }

    @Override
    public String createCheckoutSession(Organization organization, OrganizationPlan plan, String successUrl, String cancelUrl) {
        if (isMockMode()) {
            log.warn("Stripe API key is a placeholder. Returning simulated Stripe Checkout session URL for testing.");
            String mockSessionId = "mock_cs_" + UUID.randomUUID();
            return successUrl.contains("{CHECKOUT_SESSION_ID}")
                    ? successUrl.replace("{CHECKOUT_SESSION_ID}", mockSessionId)
                    : successUrl + (successUrl.contains("?") ? "&" : "?") + "session_id=" + mockSessionId;
        }

        try {
            String customerId = resolveOrCreateCustomerId(organization);

            long amount = (plan == OrganizationPlan.BUSINESS) ? businessAmount : starterAmount;
            String priceId = (plan == OrganizationPlan.BUSINESS) ? businessPriceId : starterPriceId;

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(organization.getId().toString())
                    .putMetadata("organizationId", organization.getId().toString())
                    .putMetadata("targetPlan", plan.name());

            if (priceId != null && priceId.startsWith("price_") && !priceId.contains("test")) {
                builder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                );
            } else {
                // Utilise price_data inline pour permettre le checkout sans créer de produit Stripe au préalable
                builder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("xof")
                                                .setUnitAmount(amount)
                                                .setRecurring(
                                                        SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                                .build()
                                                )
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Prospecta " + plan.name())
                                                                .setDescription("Abonnement mensuel Prospecta B2B — " + plan.name())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );
            }

            Session session = Session.create(builder.build());
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Failed to create Stripe Checkout session for org {}: {}", organization.getId(), e.getMessage(), e);
            throw new BillingException("STRIPE_CHECKOUT_ERROR", "Erreur lors de la création de la session de paiement: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public String createCustomerPortalSession(String stripeCustomerId, String returnUrl) {
        if (isMockMode()) {
            log.warn("Stripe API key is a placeholder. Returning simulated Stripe Customer Portal URL.");
            return returnUrl + (returnUrl.contains("?") ? "&" : "?") + "portal_session=mock_active";
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(returnUrl)
                            .build();

            com.stripe.model.billingportal.Session portalSession =
                    com.stripe.model.billingportal.Session.create(params);

            return portalSession.getUrl();

        } catch (StripeException e) {
            log.error("Failed to create Stripe Customer Portal session for customer {}: {}", stripeCustomerId, e.getMessage(), e);
            throw new BillingException("STRIPE_PORTAL_ERROR", "Erreur lors de l'accès au portail de facturation: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private String resolveOrCreateCustomerId(Organization organization) throws StripeException {
        if (organization.getStripeCustomerId() != null && !organization.getStripeCustomerId().isBlank()) {
            return organization.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(organization.getName())
                .setEmail(organization.getEmail())
                .putMetadata("organizationId", organization.getId().toString())
                .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }
}
