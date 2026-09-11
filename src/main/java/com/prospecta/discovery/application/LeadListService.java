package com.prospecta.discovery.application;

import com.prospecta.discovery.infrastructure.persistence.LeadList;
import com.prospecta.discovery.infrastructure.persistence.LeadListMember;
import com.prospecta.discovery.infrastructure.persistence.LeadListMemberRepository;
import com.prospecta.discovery.infrastructure.persistence.LeadListRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadListService {

    private final LeadListRepository leadListRepository;
    private final LeadListMemberRepository leadListMemberRepository;
    private final ProspectRepository prospectRepository;

    @Transactional
    public LeadList createLeadList(String name, String description) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de la liste est obligatoire");
        }

        if (leadListRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, name.trim())) {
            throw new DuplicateResourceException("Une liste nommée '" + name.trim() + "' existe déjà dans votre organisation");
        }

        LeadList leadList = LeadList.builder()
                .name(name.trim())
                .description(description != null ? description.trim() : null)
                .build();
        leadList.setOrganizationId(organizationId);

        LeadList saved = leadListRepository.save(leadList);
        log.info("Lead list created [id={}, name='{}'] for organization [{}]", saved.getId(), saved.getName(), organizationId);
        return saved;
    }

    @Transactional(readOnly = true)
    public LeadList getLeadList(UUID listId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        return leadListRepository.findByIdAndOrganizationId(listId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException("Liste de prospects introuvable"));
    }

    @Transactional(readOnly = true)
    public Page<LeadList> getLeadLists(Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        return leadListRepository.findAllByOrganizationId(organizationId, pageable);
    }

    @Transactional
    public void addProspectsToList(UUID listId, List<UUID> prospectIds) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        LeadList leadList = leadListRepository.findByIdAndOrganizationId(listId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException("Liste de prospects introuvable"));

        if (prospectIds == null || prospectIds.isEmpty()) {
            return;
        }

        List<LeadListMember> newMembers = new ArrayList<>();
        for (UUID prospectId : prospectIds) {
            // Verify prospect belongs to tenant
            Prospect prospect = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId).orElse(null);
            if (prospect != null && !leadListMemberRepository.existsByLeadListIdAndProspectId(listId, prospectId)) {
                LeadListMember member = LeadListMember.builder()
                        .leadList(leadList)
                        .prospect(prospect)
                        .build();
                newMembers.add(member);
            }
        }

        if (!newMembers.isEmpty()) {
            leadListMemberRepository.saveAll(newMembers);
            log.info("Added {} prospects to lead list [id={}, name='{}']", newMembers.size(), listId, leadList.getName());
        }
    }

    @Transactional(readOnly = true)
    public Page<Prospect> getProspectsInList(UUID listId, Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        // Verify list ownership
        leadListRepository.findByIdAndOrganizationId(listId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException("Liste de prospects introuvable"));

        Page<LeadListMember> memberPage = leadListMemberRepository.findAllByLeadListId(listId, pageable);
        List<Prospect> prospects = memberPage.getContent().stream()
                .map(LeadListMember::getProspect)
                .toList();

        return new PageImpl<>(prospects, pageable, memberPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public long countMembers(UUID listId) {
        return leadListMemberRepository.countByLeadListId(listId);
    }

    @Transactional
    public void removeProspectFromList(UUID listId, UUID prospectId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        leadListRepository.findByIdAndOrganizationId(listId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException("Liste de prospects introuvable"));

        leadListMemberRepository.deleteByLeadListIdAndProspectId(listId, prospectId);
        log.info("Removed prospect [{}] from lead list [{}]", prospectId, listId);
    }
}
