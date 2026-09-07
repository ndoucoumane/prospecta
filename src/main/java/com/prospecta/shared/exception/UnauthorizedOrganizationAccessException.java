package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOrganizationAccessException extends BusinessException {

    public UnauthorizedOrganizationAccessException(String message) {
        super(message, "UNAUTHORIZED_ORGANIZATION_ACCESS", HttpStatus.FORBIDDEN);
    }
}
