package com.prospecta.prospect.repository;

import com.prospecta.prospect.domain.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Company> findByOrganizationIdAndExternalId(UUID organizationId, String externalId);

    Optional<Company> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    Optional<Company> findByOrganizationIdAndWebsite(UUID organizationId, String website);

    Page<Company> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
