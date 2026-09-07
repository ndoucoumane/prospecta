package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CampaignNotFoundException extends BusinessException {

    public CampaignNotFoundException(UUID campaignId) {
        super("Campaign not found with ID: " + campaignId, "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public CampaignNotFoundException(String message) {
        super(message, "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
