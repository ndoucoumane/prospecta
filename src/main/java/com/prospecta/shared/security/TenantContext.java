package com.prospecta.shared.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TenantContext {
    private final UUID organizationId;
    private final UserPrincipal userPrincipal;
    private final String traceId;

    public static TenantContext of(UUID organizationId, UserPrincipal userPrincipal, String traceId) {
        return TenantContext.builder()
                .organizationId(organizationId)
                .userPrincipal(userPrincipal)
                .traceId(traceId)
                .build();
    }
}
