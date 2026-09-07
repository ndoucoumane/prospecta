package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CompanyNotFoundException extends BusinessException {

    public CompanyNotFoundException(UUID companyId) {
        super("Company not found with ID: " + companyId, "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public CompanyNotFoundException(String message) {
        super(message, "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
