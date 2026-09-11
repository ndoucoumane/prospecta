package com.prospecta.prospect.repository;

import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, UUID> {

    Optional<Prospect> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Prospect> findByOrganizationIdAndExternalId(UUID organizationId, String externalId);

    Optional<Prospect> findByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    Optional<Prospect> findByOrganizationIdAndPhone(UUID organizationId, String phone);

    Optional<Prospect> findByOrganizationIdAndWhatsappNumber(UUID organizationId, String whatsappNumber);

    Optional<Prospect> findByOrganizationIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndCompanyNameIgnoreCase(
            UUID organizationId, String firstName, String lastName, String companyName);

    Page<Prospect> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Prospect> findAllByOrganizationIdAndStatus(UUID organizationId, ProspectStatus status, Pageable pageable);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, ProspectStatus status);

    @Query("SELECT p FROM Prospect p WHERE p.organizationId = :orgId AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:search IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.companyName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Prospect> searchProspects(
            @Param("orgId") UUID orgId,
            @Param("status") ProspectStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
