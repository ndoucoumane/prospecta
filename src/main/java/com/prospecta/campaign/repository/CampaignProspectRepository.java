package com.prospecta.campaign.repository;

import com.prospecta.campaign.domain.CampaignProspect;
import com.prospecta.campaign.domain.CampaignProspectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignProspectRepository extends JpaRepository<CampaignProspect, UUID> {

    Optional<CampaignProspect> findByCampaignIdAndProspectId(UUID campaignId, UUID prospectId);

    @Query("SELECT cp FROM CampaignProspect cp " +
            "JOIN FETCH cp.campaign c " +
            "JOIN FETCH cp.prospect p " +
            "WHERE c.status = 'RUNNING' " +
            "AND cp.status = :status " +
            "AND cp.nextActionAt <= :now")
    List<CampaignProspect> findDueProspects(
            @Param("status") CampaignProspectStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    long countByCampaignIdAndStatus(UUID campaignId, CampaignProspectStatus status);

    long countByCampaignId(UUID campaignId);

    Page<CampaignProspect> findAllByCampaignId(UUID campaignId, Pageable pageable);
}
