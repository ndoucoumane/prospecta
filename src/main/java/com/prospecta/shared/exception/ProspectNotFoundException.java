package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ProspectNotFoundException extends BusinessException {

    public ProspectNotFoundException(UUID prospectId) {
        super("Prospect not found with ID: " + prospectId, "PROSPECT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ProspectNotFoundException(String message) {
        super(message, "PROSPECT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
