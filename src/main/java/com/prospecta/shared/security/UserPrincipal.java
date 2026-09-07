package com.prospecta.shared.security;

import com.prospecta.identity.domain.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class UserPrincipal {
    private final UUID userId;
    private final String keycloakSubject;
    private final UUID organizationId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final UserRole role;
    private final Set<String> permissions;

    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    public boolean isOrgAdmin() {
        return role == UserRole.ORG_ADMIN || role == UserRole.SUPER_ADMIN;
    }

    public boolean canManageSales() {
        return role == UserRole.SALES_MANAGER || role == UserRole.ORG_ADMIN || role == UserRole.SUPER_ADMIN;
    }
}
