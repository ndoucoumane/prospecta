package com.prospecta.shared.audit;

import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(String action, String resourceType, String resourceId, String metadata) {
        logSync(action, resourceType, resourceId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logSync(String action, String resourceType, String resourceId, String metadata) {
        try {
            UUID orgId = TenantContextHolder.getOrganizationId().orElse(null);
            UUID userId = TenantContextHolder.getUserPrincipal().map(UserPrincipal::getUserId).orElse(null);

            AuditLog logEntry = AuditLog.builder()
                    .organizationId(orgId)
                    .userId(userId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .metadata(metadata)
                    .build();

            auditRepository.save(logEntry);
            log.info("AUDIT: [action={}, resourceType={}, resourceId={}, orgId={}, userId={}]",
                    action, resourceType, resourceId, orgId, userId);
        } catch (Exception e) {
            log.error("Failed to persist audit log for action: {}", action, e);
        }
    }
}
