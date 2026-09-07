package com.prospecta.campaign.repository;

import com.prospecta.campaign.domain.Campaign;
import com.prospecta.campaign.domain.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Optional<Campaign> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Campaign> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    List<Campaign> findByStatus(CampaignStatus status);

    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.steps WHERE c.id = :id AND c.organizationId = :organizationId")
    Optional<Campaign> findByIdAndOrganizationIdWithSteps(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, CampaignStatus status);
}
