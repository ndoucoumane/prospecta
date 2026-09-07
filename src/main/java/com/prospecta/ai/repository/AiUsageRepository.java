package com.prospecta.ai.repository;

import com.prospecta.ai.domain.AiUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    Page<AiUsage> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM AiUsage u WHERE u.organizationId = :orgId AND u.createdAt >= :since")
    long countMonthlyOperations(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(u.inputTokens + u.outputTokens), 0) FROM AiUsage u WHERE u.organizationId = :orgId AND u.createdAt >= :since")
    long sumMonthlyTokens(@Param("orgId") UUID orgId, @Param("since") Instant since);
}
