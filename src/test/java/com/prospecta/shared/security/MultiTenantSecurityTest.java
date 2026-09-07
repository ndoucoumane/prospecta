package com.prospecta.shared.security;

import com.prospecta.identity.domain.UserRole;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.organization.service.OrganizationService;
import com.prospecta.shared.exception.UnauthorizedOrganizationAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MultiTenantSecurityTest {

    @Mock
    private OrganizationRepository organizationRepository;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        organizationService = new OrganizationService(organizationRepository, null, null);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Tenant Isolation: User A from Organization A must NOT be able to access Organization B")
    void testTenantIsolationViolationThrowsForbidden() {
        UUID orgAId = UUID.randomUUID();
        UUID orgBId = UUID.randomUUID();

        UserPrincipal userA = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .keycloakSubject("keycloak-user-a")
                .organizationId(orgAId)
                .email("user.a@orga.sn")
                .role(UserRole.SALES_REP)
                .permissions(Collections.emptySet())
                .build();

        TenantContextHolder.setContext(TenantContext.of(orgAId, userA, "test-trace-1"));

        // Attempting to access Org B with User A's context must fail with UnauthorizedOrganizationAccessException
        assertThatThrownBy(() -> organizationService.validateTenantAccess(orgBId))
                .isInstanceOf(UnauthorizedOrganizationAccessException.class)
                .hasMessageContaining("You are not authorized to access organization: " + orgBId);
    }

    @Test
    @DisplayName("Tenant Isolation: User A can access Organization A")
    void testTenantAccessWithinSameOrgAllowed() {
        UUID orgAId = UUID.randomUUID();

        UserPrincipal userA = UserPrincipal.builder()
                .userId(UUID.randomUUID())
                .keycloakSubject("keycloak-user-a")
                .organizationId(orgAId)
                .email("user.a@orga.sn")
                .role(UserRole.SALES_REP)
                .permissions(Collections.emptySet())
                .build();

        TenantContextHolder.setContext(TenantContext.of(orgAId, userA, "test-trace-2"));

        assertThatNoException().isThrownBy(() -> organizationService.validateTenantAccess(orgAId));
    }
}
