package com.prospecta.billing.service;

import com.prospecta.ai.service.QuotaService;
import com.prospecta.billing.domain.SubscriptionStatus;
import com.prospecta.billing.dto.BillingPlanDto.*;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.billing.exception.BillingException;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final OrganizationRepository organizationRepository;
    private final PaymentGateway paymentGateway;

    @Transactional(readOnly = true)
    public List<BillingPlanResponse> getAvailablePlans() {
        OrganizationPlan currentPlan = TenantContextHolder.getOrganizationId()
                .flatMap(organizationRepository::findById)
                .map(Organization::getPlan)
                .orElse(OrganizationPlan.FREE);

        return List.of(
                BillingPlanResponse.builder()
                        .id(OrganizationPlan.FREE)
                        .name("Plan Gratuit (Découverte)")
                        .description("Idéal pour tester l'automatisation et le scoring de vos prospects")
                        .price(0L)
                        .currency("XOF")
                        .billingPeriod("MONTHLY")
                        .aiQuota(QuotaService.getPlanQuota(OrganizationPlan.FREE))
                        .features(List.of(
                                "1 utilisateur commercial",
                                "Jusqu'à 100 opérations IA par mois",
                                "Scoring automatique des leads UEMOA",
                                "Importation CSV jusqu'à 50 prospects"
                        ))
                        .isCurrent(currentPlan == OrganizationPlan.FREE)
                        .build(),

                BillingPlanResponse.builder()
                        .id(OrganizationPlan.STARTER)
                        .name("Plan Starter")
                        .description("Pour les commerciaux indépendants et TPE qui accélèrent leur prospection")
                        .price(29000L)
                        .currency("XOF")
                        .billingPeriod("MONTHLY")
                        .aiQuota(QuotaService.getPlanQuota(OrganizationPlan.STARTER))
                        .features(List.of(
                                "Jusqu'à 3 utilisateurs commerciaux",
                                "1 000 opérations et résumés IA par mois",
                                "Campagnes de prospection multicanales (Email & WhatsApp)",
                                "Inbox unifiée et gestion du Pipeline Kanban",
                                "Support prioritaire par WhatsApp"
                        ))
                        .isCurrent(currentPlan == OrganizationPlan.STARTER)
                        .build(),

                BillingPlanResponse.builder()
                        .id(OrganizationPlan.BUSINESS)
                        .name("Plan Business")
                        .description("Pour les équipes commerciales ambitieuses exigeant le maximum de conversion")
                        .price(79000L)
                        .currency("XOF")
                        .billingPeriod("MONTHLY")
                        .aiQuota(QuotaService.getPlanQuota(OrganizationPlan.BUSINESS))
                        .features(List.of(
                                "Jusqu'à 10 utilisateurs commerciaux",
                                "5 000 opérations et copies IA par mois",
                                "Génération automatique d'angles de vente IA",
                                "IA Copilot Inbox avec suggestions de réponses temps réel",
                                "Accès API complet et Webhooks personnalisés",
                                "Accompagnement et onboarding dédié"
                        ))
                        .isCurrent(currentPlan == OrganizationPlan.BUSINESS)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public OrganizationSubscriptionResponse getCurrentSubscription() {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BillingException("ORGANIZATION_NOT_FOUND", "Organisation introuvable", HttpStatus.NOT_FOUND));

        return OrganizationSubscriptionResponse.builder()
                .organizationId(org.getId())
                .organizationName(org.getName())
                .plan(org.getPlan())
                .status(org.getSubscriptionStatus() != null ? org.getSubscriptionStatus() : SubscriptionStatus.ACTIVE)
                .stripeCustomerId(org.getStripeCustomerId())
                .stripeSubscriptionId(org.getStripeSubscriptionId())
                .currentPeriodEnd(org.getCurrentPeriodEnd())
                .monthlyAiQuota(QuotaService.getPlanQuota(org.getPlan()))
                .build();
    }

    @Transactional
    public CheckoutResponse createCheckoutSession(CreateCheckoutRequest request) {
        if (request.getPlan() == OrganizationPlan.FREE) {
            throw new BillingException("INVALID_PLAN", "Le plan FREE ne requiert aucun paiement", HttpStatus.BAD_REQUEST);
        }

        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BillingException("ORGANIZATION_NOT_FOUND", "Organisation introuvable", HttpStatus.NOT_FOUND));

        log.info("Initiating Stripe checkout for organization [{}] towards plan [{}]", org.getId(), request.getPlan());
        String checkoutUrl = paymentGateway.createCheckoutSession(org, request.getPlan(), request.getSuccessUrl(), request.getCancelUrl());

        // Sauvegarde de l'organisation si le customer ID a été initialisé
        organizationRepository.save(org);

        return CheckoutResponse.builder()
                .checkoutUrl(checkoutUrl)
                .sessionId(extractSessionId(checkoutUrl))
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerPortalResponse createCustomerPortalSession(CustomerPortalRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BillingException("ORGANIZATION_NOT_FOUND", "Organisation introuvable", HttpStatus.NOT_FOUND));

        if (org.getStripeCustomerId() == null || org.getStripeCustomerId().isBlank()) {
            throw new BillingException("NO_STRIPE_CUSTOMER", "Aucun moyen de paiement ou compte Stripe n'est associé à cette organisation. Veuillez souscrire à un plan au préalable.", HttpStatus.BAD_REQUEST);
        }

        String portalUrl = paymentGateway.createCustomerPortalSession(org.getStripeCustomerId(), request.getReturnUrl());
        return CustomerPortalResponse.builder()
                .portalUrl(portalUrl)
                .build();
    }

    private String extractSessionId(String checkoutUrl) {
        if (checkoutUrl != null && checkoutUrl.contains("session_id=")) {
            int start = checkoutUrl.indexOf("session_id=") + 11;
            int end = checkoutUrl.indexOf("&", start);
            return end > 0 ? checkoutUrl.substring(start, end) : checkoutUrl.substring(start);
        }
        return null;
    }
}
