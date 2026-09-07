package com.prospecta.identity.service;

import com.prospecta.identity.domain.UserProfile;
import com.prospecta.identity.dto.UpdateUserProfileRequest;
import com.prospecta.identity.dto.UserProfileResponse;
import com.prospecta.identity.repository.UserProfileRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.dto.PageResponse;
import com.prospecta.shared.exception.UserProfileNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
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
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        UserPrincipal principal = TenantContextHolder.getUserPrincipal()
                .orElseThrow(() -> new UserProfileNotFoundException("No authenticated user principal"));

        UserProfile profile = userProfileRepository.findByKeycloakSubject(principal.getKeycloakSubject())
                .orElseThrow(() -> new UserProfileNotFoundException("User profile not found for Keycloak subject: " + principal.getKeycloakSubject()));

        return UserProfileResponse.from(profile);
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        UserPrincipal principal = TenantContextHolder.getUserPrincipal()
                .orElseThrow(() -> new UserProfileNotFoundException("No authenticated user principal"));

        UserProfile profile = userProfileRepository.findByKeycloakSubject(principal.getKeycloakSubject())
                .orElseThrow(() -> new UserProfileNotFoundException("User profile not found for Keycloak subject: " + principal.getKeycloakSubject()));

        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        if (request.getJobTitle() != null) {
            profile.setJobTitle(request.getJobTitle().trim());
        }

        UserProfile updated = userProfileRepository.save(profile);
        auditService.logSync("USER_PROFILE_UPDATED", "UserProfile", updated.getId().toString(), "Updated profile for: " + updated.getEmail());

        return UserProfileResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> getUserProfilesByOrganization(Pageable pageable) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        Page<UserProfile> page = userProfileRepository.findAllByOrganizationId(organizationId, pageable);
        return PageResponse.from(page.map(UserProfileResponse::from));
    }
}
