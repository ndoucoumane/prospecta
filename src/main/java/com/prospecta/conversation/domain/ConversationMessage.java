package com.prospecta.conversation.domain;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.MessageDirection;
import com.prospecta.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "conversation_messages")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 50)
    private MessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Column(name = "sender", nullable = false)
    private String sender;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "SENT";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();
}
