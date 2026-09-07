package com.prospecta.conversation.repository;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.conversation.domain.Conversation;
import com.prospecta.conversation.domain.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Conversation> findByOrganizationIdAndProspectIdAndChannel(
            UUID organizationId, UUID prospectId, ChannelType channel
    );

    @Query("SELECT c FROM Conversation c " +
            "JOIN FETCH c.prospect p " +
            "WHERE c.organizationId = :orgId AND " +
            "(:channel IS NULL OR c.channel = :channel) AND " +
            "(:status IS NULL OR c.status = :status)")
    Page<Conversation> findFiltered(
            @Param("orgId") UUID orgId,
            @Param("channel") ChannelType channel,
            @Param("status") ConversationStatus status,
            Pageable pageable
    );

    long countByOrganizationId(UUID organizationId);
}
