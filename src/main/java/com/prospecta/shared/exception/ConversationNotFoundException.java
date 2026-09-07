package com.prospecta.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ConversationNotFoundException extends BusinessException {

    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found with ID: " + conversationId, "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ConversationNotFoundException(String message) {
        super(message, "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
