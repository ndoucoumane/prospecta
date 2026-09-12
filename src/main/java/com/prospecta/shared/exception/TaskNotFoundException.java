package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TaskNotFoundException extends BusinessException {

    public TaskNotFoundException(UUID id) {
        super("Commercial task not found with ID: " + id, "TASK_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public TaskNotFoundException(String message) {
        super(message, "TASK_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
