package com.prospecta.conversation.repository;

import com.prospecta.conversation.domain.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findAllByConversationIdOrderBySentAtAsc(UUID conversationId);

    Optional<ConversationMessage> findByExternalMessageId(String externalMessageId);
}
