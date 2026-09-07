package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(String.format("%s already exists with %s: '%s'", resourceName, field, value), "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
    }
}
