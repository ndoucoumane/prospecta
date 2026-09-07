package com.prospecta.shared.security;

import com.prospecta.shared.exception.UnauthorizedOrganizationAccessException;

import java.util.Optional;
import java.util.UUID;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> getContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static UUID getRequiredOrganizationId() {
        return getContext()
                .map(TenantContext::getOrganizationId)
                .orElseThrow(() -> new UnauthorizedOrganizationAccessException("No active organization context found in current request"));
    }

    public static Optional<UUID> getOrganizationId() {
        return getContext().map(TenantContext::getOrganizationId);
    }

    public static Optional<UserPrincipal> getUserPrincipal() {
        return getContext().map(TenantContext::getUserPrincipal);
    }

    public static String getTraceId() {
        return getContext().map(TenantContext::getTraceId).orElse("N/A");
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
