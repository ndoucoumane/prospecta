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
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.OpportunityNotFoundException;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final OpportunityRepository opportunityRepository;
    private final ProspectRepository prospectRepository;
    private final CompanyRepository companyRepository;
    private final AuditService auditService;

    @Transactional
    public OpportunityResponse createOpportunity(CreateOpportunityRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        Prospect prospect = prospectRepository.findByIdAndOrganizationId(request.getProspectId(), organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(request.getProspectId()));

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findByIdAndOrganizationId(request.getCompanyId(), organizationId).orElse(null);
        } else if (prospect.getCompany() != null) {
            company = prospect.getCompany();
        }

        OpportunityStage initialStage = request.getStage() != null ? request.getStage() : OpportunityStage.NEW;
        BigDecimal value = request.getEstimatedValue() != null ? request.getEstimatedValue() : BigDecimal.ZERO;
        String currency = request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency().toUpperCase() : "XOF";
        int probability = request.getWinProbability() != null ? request.getWinProbability() : defaultProbabilityForStage(initialStage);

        Opportunity opportunity = Opportunity.builder()
                .prospect(prospect)
                .company(company)
                .assignedTo(request.getAssignedTo())
                .title(request.getTitle().trim())
                .stage(initialStage)
                .estimatedValue(value)
                .currency(currency)
                .winProbability(probability)
                .expectedCloseDate(request.getExpectedCloseDate())
                .notes(request.getNotes())
                .build();
        opportunity.setOrganizationId(organizationId);

        Opportunity saved = opportunityRepository.save(opportunity);

        // Transition prospect to OPPORTUNITY if not already WON
        if (prospect.getStatus() != ProspectStatus.WON && prospect.getStatus() != ProspectStatus.OPPORTUNITY) {
            prospect.setStatus(ProspectStatus.OPPORTUNITY);
            prospectRepository.save(prospect);
            log.info("Prospect [{}] status updated to OPPORTUNITY following deal creation", prospect.getId());
        }

        auditService.logSync("OPPORTUNITY_CREATED", "Opportunity", saved.getId().toString(),
                String.format("Title: %s, Value: %s %s, Stage: %s", saved.getTitle(), saved.getEstimatedValue(), saved.getCurrency(), saved.getStage()));

        return OpportunityResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<OpportunityResponse> getOpportunities(OpportunityStage stage, UUID assignedTo, Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<Opportunity> page = opportunityRepository.findFiltered(organizationId, stage, assignedTo, pageable);
        return PageResponse.from(page.map(OpportunityResponse::from));
    }

    @Transactional(readOnly = true)
    public OpportunityResponse getOpportunityById(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Opportunity opportunity = opportunityRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new OpportunityNotFoundException(id));
        return OpportunityResponse.from(opportunity);
    }

    @Transactional
    public OpportunityResponse updateStage(UUID id, UpdateOpportunityStageRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Opportunity opportunity = opportunityRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new OpportunityNotFoundException(id));

        OpportunityStage oldStage = opportunity.getStage();
        OpportunityStage newStage = request.getStage();
        opportunity.setStage(newStage);

        if (newStage == OpportunityStage.WON) {
            opportunity.setClosedAt(Instant.now());
            opportunity.setWinProbability(100);
            Prospect prospect = opportunity.getProspect();
            if (prospect != null) {
                prospect.setStatus(ProspectStatus.WON);
                prospectRepository.save(prospect);
            }
        } else if (newStage == OpportunityStage.LOST) {
            opportunity.setClosedAt(Instant.now());
            opportunity.setWinProbability(0);
            opportunity.setLossReason(request.getLossReason());
            Prospect prospect = opportunity.getProspect();
            if (prospect != null) {
                prospect.setStatus(ProspectStatus.LOST);
                prospectRepository.save(prospect);
            }
        } else {
            opportunity.setClosedAt(null);
            opportunity.setLossReason(null);
            opportunity.setWinProbability(defaultProbabilityForStage(newStage));
        }

        Opportunity saved = opportunityRepository.save(opportunity);

        auditService.logSync("OPPORTUNITY_STAGE_UPDATED", "Opportunity", saved.getId().toString(),
                String.format("Stage changed from %s to %s", oldStage, newStage));

        return OpportunityResponse.from(saved);
    }

    @Transactional
    public OpportunityResponse updateOpportunity(UUID id, UpdateOpportunityRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Opportunity opportunity = opportunityRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new OpportunityNotFoundException(id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            opportunity.setTitle(request.getTitle().trim());
        }
        if (request.getAssignedTo() != null) {
            opportunity.setAssignedTo(request.getAssignedTo());
        }
        if (request.getEstimatedValue() != null) {
            opportunity.setEstimatedValue(request.getEstimatedValue());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            opportunity.setCurrency(request.getCurrency().toUpperCase());
        }
        if (request.getWinProbability() != null) {
            opportunity.setWinProbability(request.getWinProbability());
        }
        if (request.getExpectedCloseDate() != null) {
            opportunity.setExpectedCloseDate(request.getExpectedCloseDate());
        }
        if (request.getNotes() != null) {
            opportunity.setNotes(request.getNotes());
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        return OpportunityResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PipelineOverviewResponse getPipelineOverview() {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        List<PipelineStageSummary> summaries = new ArrayList<>();
        for (OpportunityStage stage : OpportunityStage.values()) {
            long count = opportunityRepository.countByOrganizationIdAndStage(organizationId, stage);
            BigDecimal total = opportunityRepository.sumEstimatedValueByStage(organizationId, stage);
            summaries.add(PipelineStageSummary.builder()
                    .stage(stage)
                    .count(count)
                    .totalValue(total != null ? total : BigDecimal.ZERO)
                    .build());
        }

        long totalCount = opportunityRepository.countByOrganizationId(organizationId);
        BigDecimal totalValue = opportunityRepository.sumTotalEstimatedValue(organizationId);

        return PipelineOverviewResponse.builder()
                .totalOpportunities(totalCount)
                .totalPipelineValue(totalValue != null ? totalValue : BigDecimal.ZERO)
                .currency("XOF")
                .stages(summaries)
                .build();
    }

    private int defaultProbabilityForStage(OpportunityStage stage) {
        return switch (stage) {
            case NEW -> 10;
            case CONTACTED -> 20;
            case QUALIFIED -> 40;
            case MEETING_SCHEDULED -> 60;
            case PROPOSAL_SENT -> 75;
            case NEGOTIATION -> 90;
            case WON -> 100;
            case LOST -> 0;
        };
    }
}
