package com.prospecta.campaign.repository;

import com.prospecta.campaign.domain.CampaignStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignStepRepository extends JpaRepository<CampaignStep, UUID> {

    List<CampaignStep> findByCampaignIdOrderByPositionAsc(UUID campaignId);

    Optional<CampaignStep> findByCampaignIdAndPosition(UUID campaignId, int position);
}
