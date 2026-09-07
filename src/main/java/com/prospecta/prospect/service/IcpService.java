package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.IdealCustomerProfile;
import com.prospecta.prospect.dto.IcpDto.CreateIcpRequest;
import com.prospecta.prospect.dto.IcpDto.IcpResponse;
import com.prospecta.prospect.dto.IcpDto.UpdateIcpRequest;
import com.prospecta.prospect.repository.IcpRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.BusinessException;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IcpService {

    private final IcpRepository icpRepository;
    private final AuditService auditService;

    @Transactional
    public IcpResponse createIcp(CreateIcpRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        IdealCustomerProfile icp = IdealCustomerProfile.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .targetIndustries(request.getTargetIndustries())
                .targetCities(request.getTargetCities())
                .targetCountries(request.getTargetCountries() != null ? request.getTargetCountries() : "SN")
                .minEmployees(request.getMinEmployees() != null ? request.getMinEmployees() : 1)
                .maxEmployees(request.getMaxEmployees() != null ? request.getMaxEmployees() : 1000)
                .targetJobTitles(request.getTargetJobTitles())
                .keywords(request.getKeywords())
                .excludedIndustries(request.getExcludedIndustries())
                .build();

        icp.setOrganizationId(organizationId);
        IdealCustomerProfile saved = icpRepository.save(icp);
        auditService.logSync("ICP_CREATED", "IdealCustomerProfile", saved.getId().toString(), "Name: " + saved.getName());

        return IcpResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IcpResponse getIcpById(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        IdealCustomerProfile icp = icpRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new BusinessException("ICP not found with ID: " + id, "ICP_NOT_FOUND", HttpStatus.NOT_FOUND) {});
        return IcpResponse.from(icp);
    }

    @Transactional(readOnly = true)
    public IcpResponse getActiveIcp() {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        IdealCustomerProfile icp = icpRepository.findFirstByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .orElseThrow(() -> new BusinessException("No ICP found for active organization", "ICP_NOT_FOUND", HttpStatus.NOT_FOUND) {});
        return IcpResponse.from(icp);
    }

    @Transactional(readOnly = true)
    public PageResponse<IcpResponse> getIcps(Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<IdealCustomerProfile> page = icpRepository.findAllByOrganizationId(organizationId, pageable);
        return PageResponse.from(page.map(IcpResponse::from));
    }

    @Transactional
    public IcpResponse updateIcp(UUID id, UpdateIcpRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        IdealCustomerProfile icp = icpRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new BusinessException("ICP not found with ID: " + id, "ICP_NOT_FOUND", HttpStatus.NOT_FOUND) {});

        if (request.getName() != null) icp.setName(request.getName().trim());
        if (request.getDescription() != null) icp.setDescription(request.getDescription().trim());
        if (request.getTargetIndustries() != null) icp.setTargetIndustries(request.getTargetIndustries());
        if (request.getTargetCities() != null) icp.setTargetCities(request.getTargetCities());
        if (request.getTargetCountries() != null) icp.setTargetCountries(request.getTargetCountries());
        if (request.getMinEmployees() != null) icp.setMinEmployees(request.getMinEmployees());
        if (request.getMaxEmployees() != null) icp.setMaxEmployees(request.getMaxEmployees());
        if (request.getTargetJobTitles() != null) icp.setTargetJobTitles(request.getTargetJobTitles());
        if (request.getKeywords() != null) icp.setKeywords(request.getKeywords());
        if (request.getExcludedIndustries() != null) icp.setExcludedIndustries(request.getExcludedIndustries());

        IdealCustomerProfile updated = icpRepository.save(icp);
        auditService.logSync("ICP_UPDATED", "IdealCustomerProfile", id.toString(), "Name: " + updated.getName());

        return IcpResponse.from(updated);
    }
}
