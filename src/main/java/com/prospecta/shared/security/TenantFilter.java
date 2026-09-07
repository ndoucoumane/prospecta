package com.prospecta.shared.security;

import com.prospecta.identity.domain.UserProfile;
import com.prospecta.identity.domain.UserRole;
import com.prospecta.identity.repository.UserProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final UserProfileRepository userProfileRepository;

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_ORG_ID = "organizationId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String keycloakSubject = jwt.getSubject();
                String email = jwt.getClaimAsString("email");
                String firstName = jwt.getClaimAsString("given_name");
                String lastName = jwt.getClaimAsString("family_name");

                Optional<UserProfile> profileOpt = userProfileRepository.findByKeycloakSubjectWithOrganization(keycloakSubject);

                UUID orgId = null;
                UUID userId = null;
                UserRole role = UserRole.SALES_REP;

                Set<String> authorities = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                if (authorities.contains("ROLE_SUPER_ADMIN")) {
                    role = UserRole.SUPER_ADMIN;
                } else if (authorities.contains("ROLE_ORG_ADMIN")) {
                    role = UserRole.ORG_ADMIN;
                } else if (authorities.contains("ROLE_SALES_MANAGER")) {
                    role = UserRole.SALES_MANAGER;
                } else if (authorities.contains("ROLE_VIEWER")) {
                    role = UserRole.VIEWER;
                }

                if (profileOpt.isPresent()) {
                    UserProfile profile = profileOpt.get();
                    orgId = profile.getOrganizationId();
                    userId = profile.getId();
                    role = profile.getRole();

                    MDC.put(MDC_ORG_ID, orgId != null ? orgId.toString() : "");
                    MDC.put(MDC_USER_ID, userId.toString());
                }

                UserPrincipal principal = UserPrincipal.builder()
                        .userId(userId)
                        .keycloakSubject(keycloakSubject)
                        .organizationId(orgId)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .role(role)
                        .permissions(Collections.emptySet())
                        .build();

                TenantContext context = TenantContext.of(orgId, principal, traceId);
                TenantContextHolder.setContext(context);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_ORG_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
