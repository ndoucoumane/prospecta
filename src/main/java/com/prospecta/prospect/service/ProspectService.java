package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.CreateProspectRequest;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.dto.ProspectResponse;
import com.prospecta.prospect.dto.UpdateProspectRequest;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.ProspectNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProspectService {

    private final ProspectRepository prospectRepository;
    private final CompanyRepository companyRepository;
    private final DeduplicationService deduplicationService;
    private final LeadScoringEngine leadScoringEngine;
    private final AuditService auditService;

    @Transactional
    public ProspectResponse createProspect(CreateProspectRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        // Phone normalization E.164
        String normalizedPhone = null;
        String whatsappNumber = null;
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            normalizedPhone = PhoneNumberUtils.normalizeToE164(request.getPhone(), "SN").orElse(request.getPhone().trim());
        }
        if (request.getWhatsappNumber() != null && !request.getWhatsappNumber().isBlank()) {
            whatsappNumber = PhoneNumberUtils.normalizeToE164(request.getWhatsappNumber(), "SN").orElse(request.getWhatsappNumber().trim());
        } else if (normalizedPhone != null && normalizedPhone.matches("^\\+221(70|75|76|77|78)\\d{7}$")) {
            whatsappNumber = normalizedPhone;
        }

        // Deduplication check
        Optional<Prospect> duplicate = deduplicationService.findDuplicate(
                organizationId,
                request.getEmail(),
                normalizedPhone,
                whatsappNumber,
                request.getFirstName(),
                request.getLastName(),
                request.getCompanyName()
        );

        if (duplicate.isPresent()) {
            throw new DuplicateResourceException("Prospect", "contact info",
                    request.getEmail() != null ? request.getEmail() : normalizedPhone);
        }

        // Link company if companyId provided, or resolve/create by companyName
        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findByIdAndOrganizationId(request.getCompanyId(), organizationId)
                    .orElse(null);
        } else if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            company = companyRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, request.getCompanyName().trim())
                    .orElseGet(() -> {
                        Company newComp = Company.builder()
                                .name(request.getCompanyName().trim())
                                .website(request.getCompanyWebsite())
                                .industry(request.getIndustry())
                                .city(request.getCity())
                                .country(request.getCountry() != null ? request.getCountry() : "SN")
                                .source("PROSPECT_CREATION")
                                .build();
                        newComp.setOrganizationId(organizationId);
                        return companyRepository.save(newComp);
                    });
        }

        Prospect prospect = Prospect.builder()
                .company(company)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .companyWebsite(request.getCompanyWebsite())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .emailStatus(request.getEmail() != null ? "VALID" : "UNKNOWN")
                .phone(normalizedPhone)
                .phoneStatus(normalizedPhone != null ? "VALID" : "UNKNOWN")
                .whatsappNumber(whatsappNumber)
                .country(request.getCountry() != null ? request.getCountry() : "SN")
                .city(request.getCity() != null ? request.getCity() : "Dakar")
                .region(request.getRegion())
                .industry(request.getIndustry())
                .companySize(request.getCompanySize())
                .linkedinUrl(request.getLinkedinUrl())
                .source(request.getSource() != null ? request.getSource() : "MANUAL")
                .status(ProspectStatus.NEW)
                .build();

        prospect.setOrganizationId(organizationId);
        prospect.setFullName(prospect.computeFullName());

        // Score lead
        LeadScoreResult scoreResult = leadScoringEngine.score(prospect);
        prospect.setLeadScore(scoreResult.getScore());
        prospect.setLeadScoreLevel(scoreResult.getLevel());
        prospect.setLeadScoreReasons(String.join("; ", scoreResult.getReasons()));

        Prospect saved = prospectRepository.save(prospect);
        auditService.logSync("PROSPECT_CREATED", "Prospect", saved.getId().toString(), "Name: " + saved.getFullName());

        return ProspectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProspectResponse getProspectById(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Prospect prospect = prospectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(id));
        return ProspectResponse.from(prospect);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProspectResponse> searchProspects(ProspectStatus status, String search, Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<Prospect> page = prospectRepository.searchProspects(organizationId, status, search, pageable);
        return PageResponse.from(page.map(ProspectResponse::from));
    }

    @Transactional
    public ProspectResponse updateProspect(UUID id, UpdateProspectRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Prospect prospect = prospectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(id));

        if (request.getFirstName() != null) prospect.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) prospect.setLastName(request.getLastName().trim());
        if (request.getJobTitle() != null) prospect.setJobTitle(request.getJobTitle().trim());
        if (request.getEmail() != null) prospect.setEmail(request.getEmail().trim());
        if (request.getPhone() != null) {
            prospect.setPhone(PhoneNumberUtils.normalizeToE164(request.getPhone(), "SN").orElse(request.getPhone().trim()));
        }
        if (request.getWhatsappNumber() != null) {
            prospect.setWhatsappNumber(PhoneNumberUtils.normalizeToE164(request.getWhatsappNumber(), "SN").orElse(request.getWhatsappNumber().trim()));
        }
        if (request.getCity() != null) prospect.setCity(request.getCity().trim());
        if (request.getRegion() != null) prospect.setRegion(request.getRegion().trim());
        if (request.getIndustry() != null) prospect.setIndustry(request.getIndustry().trim());
        if (request.getLinkedinUrl() != null) prospect.setLinkedinUrl(request.getLinkedinUrl().trim());
        if (request.getStatus() != null) prospect.setStatus(request.getStatus());

        prospect.setFullName(prospect.computeFullName());

        // Re-score
        LeadScoreResult scoreResult = leadScoringEngine.score(prospect);
        prospect.setLeadScore(scoreResult.getScore());
        prospect.setLeadScoreLevel(scoreResult.getLevel());
        prospect.setLeadScoreReasons(String.join("; ", scoreResult.getReasons()));

        Prospect updated = prospectRepository.save(prospect);
        auditService.logSync("PROSPECT_UPDATED", "Prospect", id.toString(), "Updated: " + updated.getFullName());

        return ProspectResponse.from(updated);
    }

    @Transactional
    public LeadScoreResult scoreProspect(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Prospect prospect = prospectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(id));

        LeadScoreResult scoreResult = leadScoringEngine.score(prospect);
        prospect.setLeadScore(scoreResult.getScore());
        prospect.setLeadScoreLevel(scoreResult.getLevel());
        prospect.setLeadScoreReasons(String.join("; ", scoreResult.getReasons()));

        prospectRepository.save(prospect);
        auditService.logSync("PROSPECT_SCORED", "Prospect", id.toString(), "Score: " + scoreResult.getScore());

        return scoreResult;
    }

    @Transactional
    public void deleteProspect(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Prospect prospect = prospectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(id));

        prospectRepository.delete(prospect);
        auditService.logSync("PROSPECT_DELETED", "Prospect", id.toString(), "Deleted prospect: " + prospect.getFullName());
    }
}
