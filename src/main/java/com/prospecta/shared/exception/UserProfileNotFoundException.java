package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserProfileNotFoundException extends BusinessException {

    public UserProfileNotFoundException(UUID userId) {
        super("User profile not found with ID: " + userId, "USER_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public UserProfileNotFoundException(String message) {
        super(message, "USER_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
