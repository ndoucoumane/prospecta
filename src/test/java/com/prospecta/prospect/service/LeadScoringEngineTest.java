package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.IdealCustomerProfile;
import com.prospecta.prospect.domain.LeadScoreLevel;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.repository.IcpRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LeadScoringEngineTest {

    @Mock
    private IcpRepository icpRepository;

    @InjectMocks
    private LeadScoringEngine leadScoringEngine;

    @Test
    @DisplayName("High-value Dakar prospect should achieve VERY_HIGH lead score with explainable reasons")
    void shouldScoreHighValueProspect() {
        UUID orgId = UUID.randomUUID();
        Prospect prospect = Prospect.builder()
                .firstName("Fatou")
                .lastName("Sow")
                .jobTitle("Directrice Générale")
                .companyName("Hôtel Les Almadies")
                .companyWebsite("https://hotel-almadies.sn")
                .email("direction@hotel-almadies.sn")
                .phone("+221771234567")
                .whatsappNumber("+221771234567")
                .city("Dakar")
                .country("SN")
                .industry("HOSPITALITY")
                .build();
        prospect.setOrganizationId(orgId);

        IdealCustomerProfile icp = IdealCustomerProfile.builder()
                .name("Hôtels Dakar")
                .targetIndustries("HOSPITALITY, TOURISM")
                .targetCities("Dakar")
                .build();
        icp.setOrganizationId(orgId);

        LeadScoreResult result = leadScoringEngine.score(prospect, icp);

        assertThat(result.getScore()).isGreaterThanOrEqualTo(85);
        assertThat(result.getLevel()).isEqualTo(LeadScoreLevel.VERY_HIGH);
        assertThat(result.getReasons()).contains(
                "Prospect ou entreprise localisé au Sénégal / Dakar",
                "Numéro de téléphone / WhatsApp sénégalais valide (+221)",
                "Adresse email professionnelle disponible",
                "Poste de décisionnaire identifié: Directrice Générale",
                "Site web d'entreprise actif",
                "Secteur d'activité (HOSPITALITY) correspondant à l'ICP"
        );
    }

    @Test
    @DisplayName("Prospect without phone or decision maker title should receive lower score")
    void shouldScoreLowerForIncompleteProspect() {
        Prospect prospect = Prospect.builder()
                .firstName("Mamadou")
                .lastName("Diop")
                .email("mamadou@gmail.com")
                .country("FR")
                .city("Paris")
                .build();

        LeadScoreResult result = leadScoringEngine.score(prospect, null);

        assertThat(result.getScore()).isLessThan(40);
        assertThat(result.getLevel()).isEqualTo(LeadScoreLevel.LOW);
    }
}
