package com.prospecta.messaging.repository;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.Message;
import com.prospecta.messaging.domain.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Message> findByExternalId(String externalId);

    Page<Message> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Message> findAllByOrganizationIdAndChannel(UUID organizationId, ChannelType channel, Pageable pageable);

    long countByOrganizationIdAndStatus(UUID organizationId, MessageStatus status);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndChannel(UUID organizationId, ChannelType channel);

    long countByOrganizationIdAndChannelAndStatus(UUID organizationId, ChannelType channel, MessageStatus status);
}
