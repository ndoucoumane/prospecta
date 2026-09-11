package com.prospecta.enrichment.service;

import com.prospecta.enrichment.domain.EnrichedProspectData;
import com.prospecta.enrichment.domain.EnrichmentProvider;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.dto.ProspectResponse;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.prospect.service.LeadScoringEngine;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProspectEnrichmentService {

    private final ProspectRepository prospectRepository;
    private final EnrichmentProvider enrichmentProvider;
    private final LeadScoringEngine leadScoringEngine;
    private final AuditService auditService;

    @Transactional
    public ProspectResponse enrichProspect(UUID prospectId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        Prospect prospect = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException("Prospect introuvable"));

        log.info("Lancement de l'enrichissement pour le prospect [id={}, name='{}'] via {}",
                prospect.getId(), prospect.getFullName(), enrichmentProvider.getProviderName());

        Optional<EnrichedProspectData> enrichedOpt = enrichmentProvider.enrichPerson(prospect);

        if (enrichedOpt.isPresent()) {
            EnrichedProspectData enriched = enrichedOpt.get();

            if (enriched.email() != null && !enriched.email().isBlank()) {
                prospect.setEmail(enriched.email().trim());
                prospect.setEmailStatus("VERIFIED");
            }

            if (enriched.phone() != null && !enriched.phone().isBlank()) {
                String normalized = PhoneNumberUtils.normalizeToE164(enriched.phone(), "SN")
                        .orElse(enriched.phone().trim());
                prospect.setPhone(normalized);
                prospect.setPhoneStatus("VALID");

                if (normalized.matches("^\\+221(70|75|76|77|78)\\d{7}$")) {
                    prospect.setWhatsappNumber(normalized);
                }
            }

            if (enriched.mobilePhone() != null && !enriched.mobilePhone().isBlank() && prospect.getWhatsappNumber() == null) {
                String normalizedMobile = PhoneNumberUtils.normalizeToE164(enriched.mobilePhone(), "SN")
                        .orElse(enriched.mobilePhone().trim());
                if (normalizedMobile.matches("^\\+221(70|75|76|77|78)\\d{7}$")) {
                    prospect.setWhatsappNumber(normalizedMobile);
                }
            }

            if (enriched.linkedinUrl() != null && !enriched.linkedinUrl().isBlank() && prospect.getLinkedinUrl() == null) {
                prospect.setLinkedinUrl(enriched.linkedinUrl().trim());
            }

            if (enriched.companyDomain() != null && !enriched.companyDomain().isBlank() && prospect.getCompanyWebsite() == null) {
                prospect.setCompanyWebsite(enriched.companyDomain().trim());
            }

            if (enriched.jobTitle() != null && !enriched.jobTitle().isBlank() && prospect.getJobTitle() == null) {
                prospect.setJobTitle(enriched.jobTitle().trim());
            }

            if (prospect.getStatus() == ProspectStatus.NEW) {
                prospect.setStatus(ProspectStatus.QUALIFIED);
            }

            // Recalculate lead scoring with enriched attributes
            LeadScoreResult scoreResult = leadScoringEngine.score(prospect);
            prospect.setLeadScore(scoreResult.getScore());
            prospect.setLeadScoreLevel(scoreResult.getLevel());
            prospect.setLeadScoreReasons(String.join("; ", scoreResult.getReasons()));

            Prospect saved = prospectRepository.save(prospect);

            auditService.logSync(
                    "ENRICHED",
                    "PROSPECT",
                    saved.getId().toString(),
                    "Enrichi avec succès via " + enrichmentProvider.getProviderName() + " (Score: " + saved.getLeadScore() + ")"
            );

            log.info("Prospect enrichi avec succès: [id={}, score={}]", saved.getId(), saved.getLeadScore());
            return ProspectResponse.from(saved);
        } else {
            log.warn("Aucune donnée d'enrichissement trouvée pour le prospect [{}]", prospectId);
            return ProspectResponse.from(prospect);
        }
    }
}
