package com.prospecta.campaign.service;

import com.prospecta.campaign.domain.CampaignProspect;
import com.prospecta.campaign.domain.CampaignProspectStatus;
import com.prospecta.campaign.domain.CampaignStep;
import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.campaign.repository.CampaignProspectRepository;
import com.prospecta.campaign.repository.CampaignStepRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignExecutionService {

    private final CampaignProspectRepository campaignProspectRepository;
    private final CampaignStepRepository campaignStepRepository;
    private final ProspectRepository prospectRepository;
    private final OutboxService outboxService;

    @Transactional
    public int executeDueSteps() {
        Instant now = Instant.now();
        List<CampaignProspect> dueProspects = campaignProspectRepository.findDueProspects(
                CampaignProspectStatus.ACTIVE, now, PageRequest.of(0, 50)
        );

        if (dueProspects.isEmpty()) {
            return 0;
        }

        log.info("Processing {} due campaign steps", dueProspects.size());
        int processed = 0;

        for (CampaignProspect cp : dueProspects) {
            try {
                processSingleTarget(cp, now);
                processed++;
            } catch (Exception e) {
                log.error("Error executing step for campaign target [{}]: {}", cp.getId(), e.getMessage(), e);
            }
        }

        return processed;
    }

    private void processSingleTarget(CampaignProspect cp, Instant now) {
        Prospect prospect = cp.getProspect();

        // 1. Condition Evaluation: Stop if prospect has replied or opted out
        if (prospect.getStatus() == ProspectStatus.REPLIED) {
            cp.setStatus(CampaignProspectStatus.REPLIED);
            cp.setCompletedAt(now);
            campaignProspectRepository.save(cp);
            log.info("Prospect [{}] replied: stopping campaign [{}] sequence.", prospect.getId(), cp.getCampaign().getId());
            return;
        }

        if (prospect.getStatus() == ProspectStatus.OPTED_OUT) {
            cp.setStatus(CampaignProspectStatus.OPTED_OUT);
            cp.setCompletedAt(now);
            campaignProspectRepository.save(cp);
            log.info("Prospect [{}] opted out: stopping campaign [{}] sequence.", prospect.getId(), cp.getCampaign().getId());
            return;
        }

        // 2. Fetch current sequence step
        Optional<CampaignStep> stepOpt = campaignStepRepository.findByCampaignIdAndPosition(
                cp.getCampaign().getId(), cp.getCurrentStep()
        );

        if (stepOpt.isEmpty() || !stepOpt.get().isEnabled()) {
            // No more steps or step disabled: mark completed
            cp.setStatus(CampaignProspectStatus.COMPLETED);
            cp.setCompletedAt(now);
            campaignProspectRepository.save(cp);
            return;
        }

        CampaignStep step = stepOpt.get();

        // 3. Personalize message template
        String content = renderTemplate(step.getContentTemplate(), prospect);
        String subject = step.getSubjectTemplate() != null ? renderTemplate(step.getSubjectTemplate(), prospect) : null;

        // Resolve recipient address based on channel
        String recipient = resolveRecipient(step.getChannel(), prospect);

        // 4. Record transactional outbox event for messaging dispatch
        Map<String, Object> payload = new HashMap<>();
        payload.put("campaignId", cp.getCampaign().getId());
        payload.put("organizationId", cp.getCampaign().getOrganizationId());
        payload.put("prospectId", prospect.getId());
        payload.put("channel", step.getChannel().name());
        payload.put("recipient", recipient);
        payload.put("subject", subject);
        payload.put("content", content);
        payload.put("stepPosition", step.getPosition());

        outboxService.recordEvent(
                "Message", prospect.getId().toString(), "message.send.requested", payload
        );

        // 5. Update Prospect status to CONTACTED if it was NEW or QUALIFIED
        if (prospect.getStatus() == ProspectStatus.NEW || prospect.getStatus() == ProspectStatus.QUALIFIED) {
            prospect.setStatus(ProspectStatus.CONTACTED);
            prospectRepository.save(prospect);
        }

        // 6. Advance to next step or complete sequence
        int nextPosition = cp.getCurrentStep() + 1;
        Optional<CampaignStep> nextStepOpt = campaignStepRepository.findByCampaignIdAndPosition(
                cp.getCampaign().getId(), nextPosition
        );

        cp.setLastActionAt(now);
        if (nextStepOpt.isPresent() && nextStepOpt.get().isEnabled()) {
            CampaignStep nextStep = nextStepOpt.get();
            cp.setCurrentStep(nextPosition);
            cp.setNextActionAt(now.plusSeconds(nextStep.getDelayMinutes() * 60L));
            log.info("Advanced target [{}] to step [{}] (nextActionAt in {} min)",
                    cp.getId(), nextPosition, nextStep.getDelayMinutes());
        } else {
            cp.setStatus(CampaignProspectStatus.COMPLETED);
            cp.setCompletedAt(now);
            log.info("Completed all steps for target [{}] in campaign [{}]", cp.getId(), cp.getCampaign().getId());
        }

        campaignProspectRepository.save(cp);
    }

    private String renderTemplate(String template, Prospect prospect) {
        if (template == null) return "";
        return template
                .replace("{firstName}", prospect.getFirstName() != null ? prospect.getFirstName() : "")
                .replace("{lastName}", prospect.getLastName() != null ? prospect.getLastName() : "")
                .replace("{fullName}", prospect.getFullName() != null ? prospect.getFullName() : "")
                .replace("{companyName}", prospect.getCompanyName() != null ? prospect.getCompanyName() : "votre entreprise")
                .replace("{jobTitle}", prospect.getJobTitle() != null ? prospect.getJobTitle() : "")
                .replace("{city}", prospect.getCity() != null ? prospect.getCity() : "Dakar");
    }

    private String resolveRecipient(ChannelType channel, Prospect prospect) {
        return switch (channel) {
            case WHATSAPP -> prospect.getWhatsappNumber() != null ? prospect.getWhatsappNumber() : prospect.getPhone();
            case EMAIL -> prospect.getEmail();
            case SMS -> prospect.getPhone();
        };
    }
}
