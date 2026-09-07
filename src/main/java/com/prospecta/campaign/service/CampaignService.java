package com.prospecta.campaign.service;

import com.prospecta.campaign.domain.*;
import com.prospecta.campaign.dto.CampaignDto.*;
import com.prospecta.campaign.repository.CampaignProspectRepository;
import com.prospecta.campaign.repository.CampaignRepository;
import com.prospecta.campaign.repository.CampaignStepRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.CampaignNotFoundException;
import com.prospecta.shared.outbox.OutboxService;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignStepRepository campaignStepRepository;
    private final CampaignProspectRepository campaignProspectRepository;
    private final ProspectRepository prospectRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        UUID userId = TenantContextHolder.getUserPrincipal().map(UserPrincipal::getUserId).orElse(null);

        Campaign campaign = Campaign.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .channelStrategy(request.getChannelStrategy() != null ? request.getChannelStrategy() : "MULTI_CHANNEL")
                .status(CampaignStatus.DRAFT)
                .createdBy(userId)
                .build();

        campaign.setOrganizationId(organizationId);
        Campaign saved = campaignRepository.save(campaign);
        auditService.logSync("CAMPAIGN_CREATED", "Campaign", saved.getId().toString(), "Name: " + saved.getName());

        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse configureSteps(UUID campaignId, List<CampaignStepRequest> stepRequests) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationIdWithSteps(campaignId, organizationId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        campaign.getSteps().clear();
        int pos = 1;
        for (CampaignStepRequest req : stepRequests) {
            CampaignStep step = CampaignStep.builder()
                    .campaign(campaign)
                    .position(req.getPosition() > 0 ? req.getPosition() : pos++)
                    .channel(req.getChannel())
                    .delayMinutes(req.getDelayMinutes())
                    .subjectTemplate(req.getSubjectTemplate())
                    .contentTemplate(req.getContentTemplate().trim())
                    .conditions(req.getConditions())
                    .enabled(req.isEnabled())
                    .build();
            campaign.addStep(step);
        }

        if (campaign.getStatus() == CampaignStatus.DRAFT && !campaign.getSteps().isEmpty()) {
            campaign.setStatus(CampaignStatus.READY);
        }

        Campaign saved = campaignRepository.save(campaign);
        auditService.logSync("CAMPAIGN_STEPS_CONFIGURED", "Campaign", campaignId.toString(), "Steps count: " + saved.getSteps().size());

        return CampaignResponse.from(saved);
    }

    @Transactional
    public int addProspects(UUID campaignId, AddProspectsRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationId(campaignId, organizationId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        int added = 0;
        for (UUID prospectId : request.getProspectIds()) {
            Optional<Prospect> prospectOpt = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId);
            if (prospectOpt.isPresent()) {
                Prospect prospect = prospectOpt.get();
                Optional<CampaignProspect> existing = campaignProspectRepository.findByCampaignIdAndProspectId(campaignId, prospectId);
                if (existing.isEmpty()) {
                    CampaignProspect cp = CampaignProspect.builder()
                            .campaign(campaign)
                            .prospect(prospect)
                            .status(CampaignProspectStatus.PENDING)
                            .currentStep(1)
                            .build();
                    campaignProspectRepository.save(cp);
                    added++;
                }
            }
        }

        auditService.logSync("CAMPAIGN_TARGETS_ADDED", "Campaign", campaignId.toString(), "Added targets: " + added);
        return added;
    }

    @Transactional
    public CampaignResponse launchCampaign(UUID campaignId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationIdWithSteps(campaignId, organizationId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        if (campaign.getSteps().isEmpty()) {
            throw new IllegalStateException("Cannot launch campaign without sequence steps configured");
        }

        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setStartedAt(Instant.now());
        Campaign saved = campaignRepository.save(campaign);

        // Activate all PENDING prospects and set nextActionAt to now for immediate Step 1 execution
        Instant now = Instant.now();
        List<CampaignProspect> pendingProspects = campaignProspectRepository.findAllByCampaignId(campaignId, Pageable.unpaged()).getContent();
        for (CampaignProspect cp : pendingProspects) {
            if (cp.getStatus() == CampaignProspectStatus.PENDING) {
                cp.setStatus(CampaignProspectStatus.ACTIVE);
                cp.setNextActionAt(now);
                campaignProspectRepository.save(cp);
            }
        }

        // Transactional Outbox event
        outboxService.recordEvent(
                "Campaign", campaignId.toString(), "campaign.started",
                Map.of("campaignId", campaignId, "organizationId", organizationId, "targetsCount", pendingProspects.size())
        );

        auditService.logSync("CAMPAIGN_LAUNCHED", "Campaign", campaignId.toString(), "Status: RUNNING");
        log.info("Campaign launched: id={}, name='{}', targets={}", saved.getId(), saved.getName(), pendingProspects.size());

        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse pauseCampaign(UUID campaignId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationId(campaignId, organizationId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        campaign.setStatus(CampaignStatus.PAUSED);
        Campaign saved = campaignRepository.save(campaign);

        outboxService.recordEvent(
                "Campaign", campaignId.toString(), "campaign.paused",
                Map.of("campaignId", campaignId, "organizationId", organizationId)
        );

        auditService.logSync("CAMPAIGN_PAUSED", "Campaign", campaignId.toString(), "Status: PAUSED");
        return CampaignResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(UUID campaignId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationIdWithSteps(campaignId, organizationId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));
        return CampaignResponse.from(campaign);
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> getCampaigns(Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<Campaign> page = campaignRepository.findAllByOrganizationId(organizationId, pageable);
        return PageResponse.from(page.map(CampaignResponse::from));
    }
}
