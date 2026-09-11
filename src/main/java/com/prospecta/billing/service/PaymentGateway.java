package com.prospecta.billing.service;

import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;

public interface PaymentGateway {

    /**
     * Crée une session de paiement Stripe Checkout pour un abonnement
     *
     * @param organization L'organisation souscriptrice
     * @param plan Le plan ciblé (STARTER ou BUSINESS)
     * @param successUrl URL de redirection après succès
     * @param cancelUrl URL de redirection en cas d'abandon
     * @return L'URL de paiement Stripe Checkout
     */
    String createCheckoutSession(Organization organization, OrganizationPlan plan, String successUrl, String cancelUrl);

    /**
     * Crée une session pour le portail client Stripe (Customer Portal)
     *
     * @param stripeCustomerId L'ID client Stripe
     * @param returnUrl URL de retour vers Prospecta
     * @return L'URL du portail client Stripe
     */
    String createCustomerPortalSession(String stripeCustomerId, String returnUrl);
}
