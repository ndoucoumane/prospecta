package com.prospecta.billing.exception;

import com.prospecta.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BillingException extends BusinessException {

    public BillingException(String errorCode, String message, HttpStatus status) {
        super(message, errorCode, status);
    }

    public BillingException(String errorCode, String message, Throwable cause, HttpStatus status) {
        super(message, cause, errorCode, status);
    }
}
