package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

public class QuotaExceededException extends BusinessException {

    public QuotaExceededException(String message) {
        super(message, "QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
    }
}
