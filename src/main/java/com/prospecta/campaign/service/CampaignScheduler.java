package com.prospecta.campaign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private final CampaignExecutionService campaignExecutionService;

    @Scheduled(fixedDelayString = "${prospecta.campaign.scheduler-interval-ms:10000}")
    public void runCampaignSteps() {
        try {
            int executed = campaignExecutionService.executeDueSteps();
            if (executed > 0) {
                log.info("Campaign scheduler executed {} actions successfully.", executed);
            }
        } catch (Exception e) {
            log.error("Campaign scheduler encounter an error: {}", e.getMessage(), e);
        }
    }
}
