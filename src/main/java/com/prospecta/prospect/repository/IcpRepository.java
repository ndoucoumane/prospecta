package com.prospecta.prospect.repository;

import com.prospecta.prospect.domain.IdealCustomerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IcpRepository extends JpaRepository<IdealCustomerProfile, UUID> {

    Optional<IdealCustomerProfile> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<IdealCustomerProfile> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<IdealCustomerProfile> findFirstByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
