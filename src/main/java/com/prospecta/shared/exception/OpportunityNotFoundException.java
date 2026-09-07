package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OpportunityNotFoundException extends BusinessException {

    public OpportunityNotFoundException(UUID opportunityId) {
        super("Opportunity not found with ID: " + opportunityId, "OPPORTUNITY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public OpportunityNotFoundException(String message) {
        super(message, "OPPORTUNITY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
