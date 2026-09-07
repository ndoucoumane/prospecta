package com.prospecta.pipeline.repository;

import com.prospecta.pipeline.domain.Opportunity;
import com.prospecta.pipeline.domain.OpportunityStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    Optional<Opportunity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
        SELECT o FROM Opportunity o
        WHERE o.organizationId = :orgId
          AND (:stage IS NULL OR o.stage = :stage)
          AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo)
        ORDER BY o.createdAt DESC
    """)
    Page<Opportunity> findFiltered(
            @Param("orgId") UUID orgId,
            @Param("stage") OpportunityStage stage,
            @Param("assignedTo") UUID assignedTo,
            Pageable pageable
    );

    List<Opportunity> findAllByOrganizationIdAndStage(UUID organizationId, OpportunityStage stage);

    List<Opportunity> findAllByOrganizationIdAndProspectId(UUID organizationId, UUID prospectId);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStage(UUID organizationId, OpportunityStage stage);

    @Query("SELECT COALESCE(SUM(o.estimatedValue), 0) FROM Opportunity o WHERE o.organizationId = :orgId AND o.stage = :stage")
    BigDecimal sumEstimatedValueByStage(@Param("orgId") UUID orgId, @Param("stage") OpportunityStage stage);

    @Query("SELECT COALESCE(SUM(o.estimatedValue), 0) FROM Opportunity o WHERE o.organizationId = :orgId")
    BigDecimal sumTotalEstimatedValue(@Param("orgId") UUID orgId);
}
