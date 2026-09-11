package com.prospecta.discovery.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadListMemberRepository extends JpaRepository<LeadListMember, UUID> {

    boolean existsByLeadListIdAndProspectId(UUID leadListId, UUID prospectId);

    long countByLeadListId(UUID leadListId);

    Page<LeadListMember> findAllByLeadListId(UUID leadListId, Pageable pageable);

    @Query("SELECT m.prospect.id FROM LeadListMember m WHERE m.leadList.id = :listId")
    List<UUID> findProspectIdsByLeadListId(@Param("listId") UUID listId);

    void deleteByLeadListIdAndProspectId(UUID leadListId, UUID prospectId);
}
