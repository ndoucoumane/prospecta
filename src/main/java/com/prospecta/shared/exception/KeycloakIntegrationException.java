package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

public class KeycloakIntegrationException extends BusinessException {

    public KeycloakIntegrationException(String message) {
        super(message, "KEYCLOAK_INTEGRATION_ERROR", HttpStatus.BAD_GATEWAY);
    }

    public KeycloakIntegrationException(String message, HttpStatus status) {
        super(message, "KEYCLOAK_INTEGRATION_ERROR", status);
    }
}
