package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.IdealCustomerProfile;
import com.prospecta.prospect.domain.LeadScoreLevel;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.repository.IcpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadScoringEngine {

    private final IcpRepository icpRepository;

    private static final Set<String> DECISION_MAKER_KEYWORDS = Set.of(
            "directeur", "directrice", "director", "dg", "ceo", "fondateur", "fondatrice", "founder",
            "gérant", "gérante", "manager", "responsable", "head", "président", "présidente", "president",
            "vp", "chief", "propriétaire", "owner"
    );

    public LeadScoreResult score(Prospect prospect) {
        Optional<IdealCustomerProfile> icpOpt = icpRepository
                .findFirstByOrganizationIdOrderByCreatedAtDesc(prospect.getOrganizationId());

        return score(prospect, icpOpt.orElse(null));
    }

    public LeadScoreResult score(Prospect prospect, IdealCustomerProfile icp) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 1. Location Fit (Dakar / Senegal)
        String city = prospect.getCity();
        String country = prospect.getCountry();
        if ((city != null && city.toLowerCase().contains("dakar")) ||
                (country != null && ("SN".equalsIgnoreCase(country) || "senegal".equalsIgnoreCase(country)))) {
            score += 20;
            reasons.add("Prospect ou entreprise localisé au Sénégal / Dakar");
        }

        // 2. Contact Quality: Mobile / WhatsApp Senegalese number verified
        String phone = prospect.getPhone();
        String whatsapp = prospect.getWhatsappNumber();
        if ((whatsapp != null && whatsapp.startsWith("+221")) || (phone != null && phone.startsWith("+221"))) {
            score += 25;
            reasons.add("Numéro de téléphone / WhatsApp sénégalais valide (+221)");
        }

        // 3. Contact Quality: Email provided
        if (prospect.getEmail() != null && !prospect.getEmail().isBlank()) {
            score += 15;
            reasons.add("Adresse email professionnelle disponible");
        }

        // 4. Decision Maker Job Title Fit
        String title = prospect.getJobTitle();
        if (title != null && !title.isBlank()) {
            String lowerTitle = title.toLowerCase();
            boolean isDecisionMaker = DECISION_MAKER_KEYWORDS.stream().anyMatch(lowerTitle::contains);
            if (isDecisionMaker) {
                score += 20;
                reasons.add("Poste de décisionnaire identifié: " + title);
            } else {
                score += 10;
                reasons.add("Titre professionnel renseigné: " + title);
            }
        }

        // 5. Digital Presence: Website or Company details
        if (prospect.getCompanyWebsite() != null && !prospect.getCompanyWebsite().isBlank()) {
            score += 10;
            reasons.add("Site web d'entreprise actif");
        } else if (prospect.getCompanyName() != null && !prospect.getCompanyName().isBlank()) {
            score += 5;
            reasons.add("Nom d'entreprise identifié");
        }

        // 6. ICP Matching if defined
        if (icp != null) {
            boolean matchedIcp = false;
            // Target Industries
            if (icp.getTargetIndustries() != null && prospect.getIndustry() != null) {
                String targetInds = icp.getTargetIndustries().toLowerCase();
                if (targetInds.contains(prospect.getIndustry().toLowerCase())) {
                    score += 10;
                    reasons.add("Secteur d'activité (" + prospect.getIndustry() + ") correspondant à l'ICP");
                    matchedIcp = true;
                }
            }

            // Target Cities
            if (icp.getTargetCities() != null && prospect.getCity() != null) {
                String targetCities = icp.getTargetCities().toLowerCase();
                if (targetCities.contains(prospect.getCity().toLowerCase()) && !matchedIcp) {
                    score += 5;
                    reasons.add("Ville (" + prospect.getCity() + ") ciblée par l'ICP");
                }
            }
        }

        // Bound score between 0 and 100
        int finalScore = Math.min(100, Math.max(0, score));
        LeadScoreLevel level = LeadScoreLevel.fromScore(finalScore);

        return LeadScoreResult.builder()
                .score(finalScore)
                .level(level)
                .reasons(reasons)
                .build();
    }
}
