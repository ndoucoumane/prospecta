# Prospecta Backend — Multi-Tenancy Architecture

## 1. Multi-Tenant Philosophy

Prospecta is architected as a **Pooled Database Multi-Tenant SaaS** (shared schema, discriminator column). This model provides optimal operational efficiency and rapid scaling while enforcing cryptographic and programmatic tenant isolation.

Every business entity directly or transitively belongs to an `Organization`:

$$\text{All Business Records} \implies \text{organization\_id UUID NOT NULL}$$

---

## 2. Core Tenancy Components

### 2.1 `TenantAwareEntity`
All tenant-scoped domain entities (`Prospect`, `Company`, `Campaign`, `Conversation`, `Opportunity`, `AiUsage`, `CommunicationConsent`) inherit from `TenantAwareEntity`:

```java
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;
}
```

- `updatable = false`: Prevents inadvertent or malicious cross-tenant reassignment in database updates.
- Indexing: Every table containing `organization_id` features composite indexes prefixed with `organization_id` (e.g., `(organization_id, status)`, `(organization_id, created_at)`).

### 2.2 `TenantContextHolder`
A thread-bound context manager providing static access to the active tenant:

```java
public final class TenantContextHolder {
    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    public static UUID getRequiredOrganizationId() {
        TenantContext ctx = CONTEXT.get();
        if (ctx == null || ctx.getOrganizationId() == null) {
            throw new UnauthorizedOrganizationAccessException("No active organization context found");
        }
        return ctx.getOrganizationId();
    }
}
```

### 2.3 `TenantFilter`
Servlet filter executing after Spring Security's `BearerTokenAuthenticationFilter`:
1. Extracts `organization_id` and user claims from the verified Keycloak JWT.
2. Initializes `TenantContextHolder`.
3. Injects `X-Trace-Id` and `X-Organization-Id` into SLF4J MDC for logging observability.
4. Guaranteed cleanup in a `finally` block to prevent thread pool contamination across HTTP requests.

---

## 3. Query & Isolation Patterns

### Pattern A: Filtered Queries
Every query executed via Spring Data JPA scopes its predicate to `organizationId`:

```java
Page<Prospect> findAllByOrganizationId(UUID organizationId, Pageable pageable);
```

### Pattern B: ID Lookups with Tenant Verification
When retrieving an entity by its primary key, the service method validates that the requested resource is owned by the caller's organization:

```java
public Prospect getProspectById(UUID id) {
    UUID orgId = TenantContextHolder.getRequiredOrganizationId();
    return prospectRepository.findByIdAndOrganizationId(id, orgId)
            .orElseThrow(() -> new ProspectNotFoundException(id));
}
```

If an ID exists in the database but belongs to a different organization, the method returns a 404 (or 403 upon security check), ensuring zero information disclosure between competitors.
