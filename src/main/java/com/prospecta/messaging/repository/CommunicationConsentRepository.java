package com.prospecta.messaging.repository;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.CommunicationConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunicationConsentRepository extends JpaRepository<CommunicationConsent, UUID> {

    Optional<CommunicationConsent> findByOrganizationIdAndProspectIdAndChannel(
            UUID organizationId, UUID prospectId, ChannelType channel
    );
}
