package com.prospecta.task.repository;

import com.prospecta.task.domain.CommercialTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommercialTaskRepository extends JpaRepository<CommercialTask, UUID> {

    Optional<CommercialTask> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT t FROM CommercialTask t WHERE t.organizationId = :organizationId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:prospectId IS NULL OR t.prospectId = :prospectId) " +
           "ORDER BY t.dueDate ASC, t.dueTime ASC, t.createdAt DESC")
    List<CommercialTask> findFiltered(
            @Param("organizationId") UUID organizationId,
            @Param("status") String status,
            @Param("prospectId") UUID prospectId
    );

    void deleteByIdAndOrganizationId(UUID id, UUID organizationId);
}
