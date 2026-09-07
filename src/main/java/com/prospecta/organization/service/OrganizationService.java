package com.prospecta.organization.service;

import com.prospecta.identity.domain.UserProfile;
import com.prospecta.identity.domain.UserRole;
import com.prospecta.identity.domain.UserStatus;
import com.prospecta.identity.repository.UserProfileRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.domain.OrganizationStatus;
import com.prospecta.organization.dto.CreateOrganizationRequest;
import com.prospecta.organization.dto.OrganizationResponse;
import com.prospecta.organization.dto.UpdateOrganizationRequest;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.OrganizationNotFoundException;
import com.prospecta.shared.exception.UnauthorizedOrganizationAccessException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? slugify(request.getSlug())
                : generateUniqueSlug(request.getName());

        if (organizationRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Organization", "slug", slug);
        }

        Organization organization = Organization.builder()
                .name(request.getName().trim())
                .slug(slug)
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().toUpperCase() : "SN")
                .timezone(request.getTimezone() != null && !request.getTimezone().isBlank() ? request.getTimezone() : "Africa/Dakar")
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency().toUpperCase() : "XOF")
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(OrganizationStatus.ACTIVE)
                .plan(OrganizationPlan.FREE)
                .build();

        Organization saved = organizationRepository.save(organization);
        log.info("Created organization: id={}, name='{}', slug='{}'", saved.getId(), saved.getName(), saved.getSlug());

        // Associate creator as ORG_ADMIN if authenticated
        Optional<UserPrincipal> principalOpt = TenantContextHolder.getUserPrincipal();
        if (principalOpt.isPresent()) {
            UserPrincipal principal = principalOpt.get();
            if (principal.getKeycloakSubject() != null) {
                Optional<UserProfile> existingProfile = userProfileRepository.findByKeycloakSubject(principal.getKeycloakSubject());
                if (existingProfile.isEmpty()) {
                    UserProfile profile = UserProfile.builder()
                            .organization(saved)
                            .keycloakSubject(principal.getKeycloakSubject())
                            .email(principal.getEmail() != null ? principal.getEmail() : "admin@" + slug + ".sn")
                            .firstName(principal.getFirstName())
                            .lastName(principal.getLastName())
                            .role(UserRole.ORG_ADMIN)
                            .status(UserStatus.ACTIVE)
                            .build();
                    userProfileRepository.save(profile);
                    log.info("Linked creator user profile id={} as ORG_ADMIN to organization id={}", profile.getId(), saved.getId());
                }
            }
        }

        auditService.logSync("ORGANIZATION_CREATED", "Organization", saved.getId().toString(), "Slug: " + saved.getSlug());

        return OrganizationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        validateTenantAccess(id);
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        return OrganizationResponse.from(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        UUID orgId = TenantContextHolder.getRequiredOrganizationId();
        return getOrganizationById(orgId);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
        validateTenantAccess(id);
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));

        if (request.getName() != null && !request.getName().isBlank()) {
            organization.setName(request.getName().trim());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            organization.setTimezone(request.getTimezone());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            organization.setCurrency(request.getCurrency().toUpperCase());
        }
        if (request.getIndustry() != null) {
            organization.setIndustry(request.getIndustry());
        }
        if (request.getWebsite() != null) {
            organization.setWebsite(request.getWebsite());
        }
        if (request.getPhone() != null) {
            organization.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            organization.setEmail(request.getEmail());
        }

        Organization updated = organizationRepository.save(organization);
        auditService.logSync("ORGANIZATION_UPDATED", "Organization", id.toString(), "Name: " + updated.getName());
        return OrganizationResponse.from(updated);
    }

    public void validateTenantAccess(UUID targetOrgId) {
        Optional<UserPrincipal> principalOpt = TenantContextHolder.getUserPrincipal();
        if (principalOpt.isEmpty()) {
            return; // In internal/unauthenticated background tasks or test mocks
        }
        UserPrincipal principal = principalOpt.get();
        if (principal.isSuperAdmin()) {
            return;
        }
        if (principal.getOrganizationId() == null || !principal.getOrganizationId().equals(targetOrgId)) {
            log.warn("Cross-tenant access attempt: User org={} tried to access target org={}", principal.getOrganizationId(), targetOrgId);
            throw new UnauthorizedOrganizationAccessException("You are not authorized to access organization: " + targetOrgId);
        }
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = slugify(name);
        String candidate = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(candidate)) {
            candidate = baseSlug + "-" + counter++;
        }
        return candidate;
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "org-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
