package com.prospecta.messaging.provider;

import com.prospecta.messaging.domain.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendResult {

    private boolean success;
    private String externalId;
    private String errorMessage;
    private MessageStatus status;

    public static MessageSendResult success(String externalId) {
        return MessageSendResult.builder()
                .success(true)
                .externalId(externalId)
                .status(MessageStatus.SENT)
                .build();
    }

    public static MessageSendResult failure(String errorMessage) {
        return MessageSendResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .status(MessageStatus.FAILED)
                .build();
    }
}
