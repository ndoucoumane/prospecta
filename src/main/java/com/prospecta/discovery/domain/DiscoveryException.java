package com.prospecta.discovery.domain;

import com.prospecta.shared.exception.BusinessException;
import lombok.Getter;

@Getter
public class DiscoveryException extends BusinessException {

    private final DiscoveryErrorCode discoveryErrorCode;

    public DiscoveryException(DiscoveryErrorCode errorCode) {
        super(errorCode.getDefaultMessage(), errorCode.getCode(), errorCode.getHttpStatus());
        this.discoveryErrorCode = errorCode;
    }

    public DiscoveryException(DiscoveryErrorCode errorCode, String customMessage) {
        super(customMessage, errorCode.getCode(), errorCode.getHttpStatus());
        this.discoveryErrorCode = errorCode;
    }

    public DiscoveryException(DiscoveryErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause, errorCode.getCode(), errorCode.getHttpStatus());
        this.discoveryErrorCode = errorCode;
    }
}
