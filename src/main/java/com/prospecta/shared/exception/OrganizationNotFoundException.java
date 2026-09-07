package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrganizationNotFoundException extends BusinessException {

    public OrganizationNotFoundException(UUID organizationId) {
        super("Organization not found with ID: " + organizationId, "ORGANIZATION_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public OrganizationNotFoundException(String message) {
        super(message, "ORGANIZATION_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
