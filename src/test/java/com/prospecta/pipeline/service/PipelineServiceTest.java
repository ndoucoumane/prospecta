package com.prospecta.pipeline.service;

import com.prospecta.pipeline.domain.Opportunity;
import com.prospecta.pipeline.domain.OpportunityStage;
import com.prospecta.pipeline.dto.OpportunityDto.*;
import com.prospecta.pipeline.repository.OpportunityRepository;
import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private ProspectRepository prospectRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PipelineService pipelineService;

    private UUID organizationId;
    private UUID prospectId;
    private UUID companyId;
    private UUID opportunityId;
    private Prospect prospect;
    private Company company;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        prospectId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        opportunityId = UUID.randomUUID();

        TenantContextHolder.setContext(TenantContext.builder()
                .organizationId(organizationId)
                .build());

        company = Company.builder()
                .name("Sénégal Logistique SA")
                .country("SN")
                .build();
        company.setId(companyId);
        company.setOrganizationId(organizationId);

        prospect = Prospect.builder()
                .fullName("Amina Ndiaye")
                .company(company)
                .companyName("Sénégal Logistique SA")
                .email("amina@senegal-logistique.sn")
                .phone("+221775551234")
                .status(ProspectStatus.REPLIED)
                .build();
        prospect.setId(prospectId);
        prospect.setOrganizationId(organizationId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Create opportunity transitions prospect status to OPPORTUNITY and calculates initial probability")
    void createOpportunity_success() {
        CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .prospectId(prospectId)
                .title("Abonnement Annuel Prospecta Entreprise")
                .stage(OpportunityStage.QUALIFIED)
                .estimatedValue(BigDecimal.valueOf(2_500_000))
                .currency("XOF")
                .expectedCloseDate(LocalDate.now().plusMonths(1))
                .build();

        when(prospectRepository.findByIdAndOrganizationId(prospectId, organizationId))
                .thenReturn(Optional.of(prospect));

        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> {
            Opportunity opp = i.getArgument(0);
            opp.setId(opportunityId);
            return opp;
        });

        OpportunityResponse response = pipelineService.createOpportunity(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Abonnement Annuel Prospecta Entreprise");
        assertThat(response.getEstimatedValue()).isEqualByComparingTo(BigDecimal.valueOf(2_500_000));
        assertThat(response.getCurrency()).isEqualTo("XOF");
        assertThat(response.getStage()).isEqualTo(OpportunityStage.QUALIFIED);
        assertThat(response.getWinProbability()).isEqualTo(40); // default for QUALIFIED
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.OPPORTUNITY);

        verify(prospectRepository).save(prospect);
        verify(opportunityRepository).save(any(Opportunity.class));
        verify(auditService).logSync(eq("OPPORTUNITY_CREATED"), eq("Opportunity"), eq(opportunityId.toString()), any());
    }

    @Test
    @DisplayName("Advancing opportunity stage to WON sets winProbability to 100% and prospect status to WON")
    void updateStage_toWon_updatesProspectToWon() {
        Opportunity opportunity = Opportunity.builder()
                .prospect(prospect)
                .company(company)
                .title("Contrat Pro")
                .stage(OpportunityStage.NEGOTIATION)
                .estimatedValue(BigDecimal.valueOf(1_800_000))
                .currency("XOF")
                .winProbability(90)
                .build();
        opportunity.setId(opportunityId);
        opportunity.setOrganizationId(organizationId);

        when(opportunityRepository.findByIdAndOrganizationId(opportunityId, organizationId))
                .thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> i.getArgument(0));

        UpdateOpportunityStageRequest stageReq = UpdateOpportunityStageRequest.builder()
                .stage(OpportunityStage.WON)
                .build();

        OpportunityResponse response = pipelineService.updateStage(opportunityId, stageReq);

        assertThat(response.getStage()).isEqualTo(OpportunityStage.WON);
        assertThat(response.getWinProbability()).isEqualTo(100);
        assertThat(response.getClosedAt()).isNotNull();
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.WON);

        verify(prospectRepository).save(prospect);
        verify(auditService).logSync(eq("OPPORTUNITY_STAGE_UPDATED"), eq("Opportunity"), eq(opportunityId.toString()), any());
    }

    @Test
    @DisplayName("Advancing opportunity stage to LOST records loss reason and sets prospect status to LOST")
    void updateStage_toLost_updatesProspectToLost() {
        Opportunity opportunity = Opportunity.builder()
                .prospect(prospect)
                .company(company)
                .title("Contrat Pro")
                .stage(OpportunityStage.PROPOSAL_SENT)
                .estimatedValue(BigDecimal.valueOf(1_800_000))
                .currency("XOF")
                .winProbability(75)
                .build();
        opportunity.setId(opportunityId);
        opportunity.setOrganizationId(organizationId);

        when(opportunityRepository.findByIdAndOrganizationId(opportunityId, organizationId))
                .thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> i.getArgument(0));

        UpdateOpportunityStageRequest stageReq = UpdateOpportunityStageRequest.builder()
                .stage(OpportunityStage.LOST)
                .lossReason("Budget gelé pour l'exercice en cours")
                .build();

        OpportunityResponse response = pipelineService.updateStage(opportunityId, stageReq);

        assertThat(response.getStage()).isEqualTo(OpportunityStage.LOST);
        assertThat(response.getWinProbability()).isEqualTo(0);
        assertThat(response.getLossReason()).isEqualTo("Budget gelé pour l'exercice en cours");
        assertThat(prospect.getStatus()).isEqualTo(ProspectStatus.LOST);

        verify(prospectRepository).save(prospect);
    }

    @Test
    @DisplayName("Get pipeline overview aggregates stage counts and total value in XOF")
    void getPipelineOverview_aggregatesCorrectly() {
        when(opportunityRepository.countByOrganizationId(organizationId)).thenReturn(8L);
        when(opportunityRepository.sumTotalEstimatedValue(organizationId)).thenReturn(BigDecimal.valueOf(16_000_000));

        when(opportunityRepository.countByOrganizationIdAndStage(eq(organizationId), any(OpportunityStage.class))).thenReturn(1L);
        when(opportunityRepository.sumEstimatedValueByStage(eq(organizationId), any(OpportunityStage.class))).thenReturn(BigDecimal.valueOf(2_000_000));

        PipelineOverviewResponse overview = pipelineService.getPipelineOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.getTotalOpportunities()).isEqualTo(8L);
        assertThat(overview.getTotalPipelineValue()).isEqualByComparingTo(BigDecimal.valueOf(16_000_000));
        assertThat(overview.getCurrency()).isEqualTo("XOF");
        assertThat(overview.getStages()).hasSize(OpportunityStage.values().length);
    }
}
