package com.prospecta.organization.service;

import com.prospecta.identity.domain.UserRole;
import com.prospecta.identity.repository.UserProfileRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.domain.OrganizationStatus;
import com.prospecta.organization.dto.CreateOrganizationRequest;
import com.prospecta.organization.dto.OrganizationResponse;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.UnauthorizedOrganizationAccessException;
import com.prospecta.shared.security.TenantContext;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should successfully create organization with slugified name")
    void shouldCreateOrganizationWithGeneratedSlug() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Agence Digitale Dakar")
                .country("SN")
                .currency("XOF")
                .build();

        when(organizationRepository.existsBySlug("agence-digitale-dakar")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            org.setId(UUID.randomUUID());
            return org;
        });

        OrganizationResponse response = organizationService.createOrganization(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Agence Digitale Dakar");
        assertThat(response.getSlug()).isEqualTo("agence-digitale-dakar");
        assertThat(response.getCountry()).isEqualTo("SN");
        assertThat(response.getCurrency()).isEqualTo("XOF");
        assertThat(response.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(response.getPlan()).isEqualTo(OrganizationPlan.FREE);

        verify(organizationRepository).save(any(Organization.class));
        verify(auditService).logSync(eq("ORGANIZATION_CREATED"), eq("Organization"), any(), any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException if custom slug already exists")
    void shouldThrowWhenSlugAlreadyExists() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Prospecta Demo")
                .slug("prospecta-demo")
                .build();

        when(organizationRepository.existsBySlug("prospecta-demo")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Organization already exists with slug: 'prospecta-demo'");

        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    @DisplayName("Should block cross-tenant access when user belongs to Org A and attempts to access Org B")
    void shouldBlockCrossTenantAccess() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();

        UserPrincipal userA = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .keycloakSubject("sub-a")
                .organizationId(orgA)
                .email("user@orga.sn")
                .role(UserRole.SALES_REP)
                .permissions(Collections.emptySet())
                .build();

        TenantContextHolder.setContext(TenantContext.of(orgA, userA, "trace-123"));

        assertThatThrownBy(() -> organizationService.getOrganizationById(orgB))
                .isInstanceOf(UnauthorizedOrganizationAccessException.class)
                .hasMessageContaining("You are not authorized to access organization: " + orgB);

        verify(organizationRepository, never()).findById(orgB);
    }

    @Test
    @DisplayName("Should allow SUPER_ADMIN to access any organization")
    void shouldAllowSuperAdminCrossTenantAccess() {
        UUID orgA = UUID.randomUUID();
        UUID targetOrg = UUID.randomUUID();

        UserPrincipal superAdmin = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .keycloakSubject("sub-superadmin")
                .organizationId(orgA)
                .email("admin@prospecta.sn")
                .role(UserRole.SUPER_ADMIN)
                .permissions(Collections.emptySet())
                .build();

        TenantContextHolder.setContext(TenantContext.of(orgA, superAdmin, "trace-123"));

        Organization org = Organization.builder()
                .name("Target Org")
                .slug("target-org")
                .build();
        org.setId(targetOrg);

        when(organizationRepository.findById(targetOrg)).thenReturn(Optional.of(org));

        OrganizationResponse response = organizationService.getOrganizationById(targetOrg);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(targetOrg);
        verify(organizationRepository).findById(targetOrg);
    }
}
