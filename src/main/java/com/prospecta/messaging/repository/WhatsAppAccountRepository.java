package com.prospecta.messaging.repository;

import com.prospecta.messaging.domain.WhatsAppAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppAccountRepository extends JpaRepository<WhatsAppAccount, UUID> {

    Optional<WhatsAppAccount> findByOrganizationId(UUID organizationId);

    Optional<WhatsAppAccount> findByPhoneNumberId(String phoneNumberId);
}
