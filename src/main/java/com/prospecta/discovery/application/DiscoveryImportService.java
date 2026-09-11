package com.prospecta.discovery.application;

import com.prospecta.discovery.domain.DiscoveredCompany;
import com.prospecta.discovery.domain.DiscoveredPerson;
import com.prospecta.discovery.infrastructure.persistence.LeadList;
import com.prospecta.discovery.web.dto.ImportPeopleReport;
import com.prospecta.discovery.web.dto.ImportPeopleRequest;
import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.prospect.service.DeduplicationService;
import com.prospecta.prospect.service.LeadScoringEngine;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryImportService {

    private final ProspectRepository prospectRepository;
    private final CompanyRepository companyRepository;
    private final DeduplicationService deduplicationService;
    private final LeadScoringEngine leadScoringEngine;
    private final LeadListService leadListService;
    private final DiscoveryResultCache discoveryResultCache;
    private final AuditService auditService;

    @Transactional
    public ImportPeopleReport importPeople(ImportPeopleRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        // 1. Resolve candidates to import
        List<DiscoveredPerson> candidates = new ArrayList<>();
        if (request.prospects() != null && !request.prospects().isEmpty()) {
            candidates.addAll(request.prospects());
        }
        if (request.externalIds() != null && !request.externalIds().isEmpty()) {
            for (String extId : request.externalIds()) {
                boolean alreadyInCandidates = candidates.stream()
                        .anyMatch(c -> Objects.equals(c.externalId(), extId));
                if (!alreadyInCandidates) {
                    discoveryResultCache.get(organizationId, extId).ifPresent(candidates::add);
                }
            }
        }

        if (candidates.isEmpty()) {
            log.warn("Aucun prospect candidat fourni ou trouvé dans le cache pour l'organisation [{}]", organizationId);
            return ImportPeopleReport.builder()
                    .importedCount(0)
                    .duplicateCount(0)
                    .totalProcessed(0)
                    .prospectIds(List.of())
                    .build();
        }

        // 2. Resolve or create LeadList if specified
        UUID targetListId = request.listId();
        String listName = null;
        if (targetListId == null && request.listName() != null && !request.listName().isBlank()) {
            LeadList createdList = leadListService.createLeadList(request.listName(), "Créée via la recherche Apollo");
            targetListId = createdList.getId();
            listName = createdList.getName();
        } else if (targetListId != null) {
            try {
                LeadList existingList = leadListService.getLeadList(targetListId);
                if (existingList != null) {
                    listName = existingList.getName();
                }
            } catch (Exception e) {
                log.warn("Could not load existing lead list [{}]: {}", targetListId, e.getMessage());
            }
        }

        int importedCount = 0;
        int duplicateCount = 0;
        List<UUID> resultProspectIds = new ArrayList<>();

        // 3. Process each discovered prospect with deduplication and tenant isolation
        for (DiscoveredPerson candidate : candidates) {
            // Check duplicate by externalId first
            Optional<Prospect> byExternalId = (candidate.externalId() != null && !candidate.externalId().isBlank())
                    ? prospectRepository.findByOrganizationIdAndExternalId(organizationId, candidate.externalId())
                    : Optional.empty();

            if (byExternalId.isPresent()) {
                duplicateCount++;
                resultProspectIds.add(byExternalId.get().getId());
                log.debug("Prospect ignoré (déjà existant par external_id [{}])", candidate.externalId());
                continue;
            }

            // Check duplicate by email, phone, name + company
            Optional<Prospect> duplicate = deduplicationService.findDuplicate(
                    organizationId,
                    candidate.email(),
                    candidate.phoneNumber(),
                    null,
                    candidate.firstName(),
                    candidate.lastName(),
                    candidate.companyName()
            );

            if (duplicate.isPresent()) {
                duplicateCount++;
                resultProspectIds.add(duplicate.get().getId());
                log.debug("Prospect ignoré (doublon détecté: {})", duplicate.get().getId());
                continue;
            }

            // Resolve or create company
            Company company = null;
            if (candidate.companyName() != null && !candidate.companyName().isBlank()) {
                company = companyRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, candidate.companyName().trim())
                        .orElseGet(() -> {
                            Company newComp = Company.builder()
                                    .name(candidate.companyName().trim())
                                    .website(candidate.companyDomain())
                                    .country(candidate.country() != null ? candidate.country() : "SN")
                                    .city(candidate.city())
                                    .source("APOLLO")
                                    .build();
                            newComp.setOrganizationId(organizationId);
                            return companyRepository.save(newComp);
                        });
            }

            // Create and persist Prospect
            Prospect prospect = Prospect.builder()
                    .company(company)
                    .externalId(candidate.externalId())
                    .firstName(candidate.firstName())
                    .lastName(candidate.lastName())
                    .fullName(candidate.fullName())
                    .jobTitle(candidate.jobTitle())
                    .companyName(candidate.companyName())
                    .companyWebsite(candidate.companyDomain())
                    .email(candidate.email())
                    .emailStatus(candidate.email() != null ? "VERIFIED" : "UNKNOWN")
                    .phone(candidate.phoneNumber())
                    .phoneStatus(candidate.phoneNumber() != null ? "VALID" : "UNKNOWN")
                    .country(candidate.country() != null ? candidate.country() : "SN")
                    .city(candidate.city())
                    .linkedinUrl(candidate.linkedinUrl())
                    .source(candidate.source() != null ? candidate.source() : "APOLLO")
                    .status(ProspectStatus.NEW)
                    .build();
            prospect.setOrganizationId(organizationId);

            LeadScoreResult scoreResult = leadScoringEngine.score(prospect);
            prospect.setLeadScore(scoreResult.getScore());
            prospect.setLeadScoreLevel(scoreResult.getLevel());
            prospect.setLeadScoreReasons(String.join("; ", scoreResult.getReasons()));

            Prospect saved = prospectRepository.save(prospect);
            resultProspectIds.add(saved.getId());
            importedCount++;

            auditService.logSync(
                    "IMPORT_DISCOVERY",
                    "PROSPECT",
                    saved.getId().toString(),
                    "Importé depuis " + saved.getSource() + " (externalId: " + saved.getExternalId() + ")"
            );
        }

        // 4. Attach imported prospects to LeadList
        if (targetListId != null && !resultProspectIds.isEmpty()) {
            leadListService.addProspectsToList(targetListId, resultProspectIds);
        }

        log.info("Importation Discovery terminée: {} nouveaux prospects importés, {} doublons, liste=[{}]",
                importedCount, duplicateCount, targetListId);

        return ImportPeopleReport.builder()
                .importedCount(importedCount)
                .duplicateCount(duplicateCount)
                .totalProcessed(candidates.size())
                .listId(targetListId)
                .listName(listName)
                .prospectIds(resultProspectIds)
                .build();
    }

    @Transactional
    public Company importCompany(DiscoveredCompany candidate) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        if (candidate.externalId() != null && !candidate.externalId().isBlank()) {
            Optional<Company> existingByExt = companyRepository.findByOrganizationIdAndExternalId(organizationId, candidate.externalId());
            if (existingByExt.isPresent()) {
                return existingByExt.get();
            }
        }

        if (candidate.name() != null && !candidate.name().isBlank()) {
            Optional<Company> existingByName = companyRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, candidate.name().trim());
            if (existingByName.isPresent()) {
                return existingByName.get();
            }
        }

        Company company = Company.builder()
                .externalId(candidate.externalId())
                .name(candidate.name())
                .website(candidate.domain())
                .industry(candidate.industry())
                .country(candidate.country() != null ? candidate.country() : "SN")
                .city(candidate.city())
                .employeeCount(candidate.employeeCount())
                .linkedinUrl(candidate.linkedinUrl())
                .source(candidate.source() != null ? candidate.source() : "APOLLO")
                .build();
        company.setOrganizationId(organizationId);

        Company saved = companyRepository.save(company);
        log.info("Company importée depuis Discovery: [id={}, name='{}']", saved.getId(), saved.getName());
        return saved;
    }
}
