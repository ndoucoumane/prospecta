package com.prospecta.discovery.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadListRepository extends JpaRepository<LeadList, UUID> {

    Optional<LeadList> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<LeadList> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    Page<LeadList> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
