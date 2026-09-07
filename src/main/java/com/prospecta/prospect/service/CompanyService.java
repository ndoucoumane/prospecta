package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.dto.CompanyDto.CompanyResponse;
import com.prospecta.prospect.dto.CompanyDto.CreateCompanyRequest;
import com.prospecta.prospect.dto.CompanyDto.UpdateCompanyRequest;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.CompanyNotFoundException;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AuditService auditService;

    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        if (companyRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, request.getName().trim())) {
            throw new DuplicateResourceException("Company", "name", request.getName());
        }

        Company company = Company.builder()
                .name(request.getName().trim())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .description(request.getDescription())
                .country(request.getCountry() != null ? request.getCountry() : "SN")
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .employeeCount(request.getEmployeeCount())
                .linkedinUrl(request.getLinkedinUrl())
                .source(request.getSource() != null ? request.getSource() : "MANUAL")
                .build();

        company.setOrganizationId(organizationId);
        Company saved = companyRepository.save(company);
        auditService.logSync("COMPANY_CREATED", "Company", saved.getId().toString(), "Name: " + saved.getName());

        return CompanyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Company company = companyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        return CompanyResponse.from(company);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> getCompanies(Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<Company> page = companyRepository.findAllByOrganizationId(organizationId, pageable);
        return PageResponse.from(page.map(CompanyResponse::from));
    }

    @Transactional
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Company company = companyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        if (request.getName() != null) company.setName(request.getName().trim());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite().trim());
        if (request.getIndustry() != null) company.setIndustry(request.getIndustry().trim());
        if (request.getDescription() != null) company.setDescription(request.getDescription().trim());
        if (request.getCity() != null) company.setCity(request.getCity().trim());
        if (request.getPhone() != null) company.setPhone(request.getPhone().trim());
        if (request.getEmail() != null) company.setEmail(request.getEmail().trim());
        if (request.getEmployeeCount() != null) company.setEmployeeCount(request.getEmployeeCount());
        if (request.getLinkedinUrl() != null) company.setLinkedinUrl(request.getLinkedinUrl().trim());

        Company updated = companyRepository.save(company);
        auditService.logSync("COMPANY_UPDATED", "Company", id.toString(), "Name: " + updated.getName());

        return CompanyResponse.from(updated);
    }
}
