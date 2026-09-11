package com.prospecta.conversation.dto;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.conversation.domain.Conversation;
import com.prospecta.conversation.domain.ConversationMessage;
import com.prospecta.conversation.domain.ConversationStatus;
import com.prospecta.messaging.domain.MessageDirection;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ConversationDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationSummaryResponse {
        private UUID id;
        private UUID prospectId;
        private String prospectName;
        private String companyName;
        private ChannelType channel;
        private String lastMessage;
        private Instant lastMessageAt;
        private ConversationStatus status;
        private UUID assignedTo;

        public static ConversationSummaryResponse from(Conversation conversation) {
            return ConversationSummaryResponse.builder()
                    .id(conversation.getId())
                    .prospectId(conversation.getProspect() != null ? conversation.getProspect().getId() : null)
                    .prospectName(conversation.getProspect() != null ? conversation.getProspect().getFullName() : "Prospect")
                    .companyName(conversation.getProspect() != null ? conversation.getProspect().getCompanyName() : "")
                    .channel(conversation.getChannel())
                    .lastMessage(conversation.getLastMessagePreview())
                    .lastMessageAt(conversation.getLastMessageAt())
                    .status(conversation.getStatus())
                    .assignedTo(conversation.getAssignedTo())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessageDto {
        private UUID id;
        private MessageDirection direction;
        private ChannelType channel;
        private String sender;
        private String recipient;
        private String content;
        private Instant sentAt;

        public static ConversationMessageDto from(ConversationMessage msg) {
            return ConversationMessageDto.builder()
                    .id(msg.getId())
                    .direction(msg.getDirection())
                    .channel(msg.getChannel())
                    .sender(msg.getSender())
                    .recipient(msg.getRecipient())
                    .content(msg.getContent())
                    .sentAt(msg.getSentAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationDetailResponse {
        private UUID id;
        private UUID prospectId;
        private String prospectName;
        private String companyName;
        private ChannelType channel;
        private ConversationStatus status;
        private UUID assignedTo;
        private List<ConversationMessageDto> messages;

        public static ConversationDetailResponse from(Conversation conv, List<ConversationMessage> messages) {
            return ConversationDetailResponse.builder()
                    .id(conv.getId())
                    .prospectId(conv.getProspect() != null ? conv.getProspect().getId() : null)
                    .prospectName(conv.getProspect() != null ? conv.getProspect().getFullName() : "Prospect")
                    .companyName(conv.getProspect() != null ? conv.getProspect().getCompanyName() : "")
                    .channel(conv.getChannel())
                    .status(conv.getStatus())
                    .assignedTo(conv.getAssignedTo())
                    .messages(messages.stream().map(ConversationMessageDto::from).toList())
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendReplyRequest {
        @NotBlank(message = "Reply content is required")
        private String content;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiReplySuggestion {
        private String suggestedReply;
        private String intent;
        private String sentiment;
        private String recommendedNextAction;
        private double confidence;
    }
}
