package com.prospecta.messaging.repository;

import com.prospecta.messaging.domain.ExternalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalEventRepository extends JpaRepository<ExternalEvent, UUID> {

    Optional<ExternalEvent> findByProviderAndExternalEventId(String provider, String externalEventId);

    boolean existsByProviderAndExternalEventId(String provider, String externalEventId);
}
