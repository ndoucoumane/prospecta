# Prospecta Backend — Security Architecture

## 1. Authentication & Identity Management

Prospecta leverages **Keycloak 24** as its centralized OpenID Connect (OIDC) Identity Provider.

- **Resource Server**: The Spring Boot backend operates strictly as an OAuth2 Resource Server.
- **JWT Verification**: Incoming HTTP requests must present a valid `Bearer <token>` issued by Keycloak.
- **Custom Claims**: Keycloak JWT tokens include:
  - `organization_id`: The tenant identifier bound to the authenticated user.
  - `realm_access.roles`: Global roles (`SUPER_ADMIN`).
  - `user_profile_id`: Internal database UUID of the user profile.

---

## 2. Multi-Tenant Isolation Enforcement

> [!IMPORTANT]
> Client HTTP requests are NEVER permitted to dictate or override the active tenant.
> The `organization_id` is extracted strictly from the validated JWT token by `TenantFilter`.

1. **Context Resolution**: `TenantFilter` runs after `BearerTokenAuthenticationFilter`, populates `TenantContextHolder` (a `ThreadLocal` storage), and clears it in a `finally` block to prevent thread pool contamination.
2. **Access Verification**: When querying or modifying resources by ID, every domain service checks:
   ```java
   if (!entity.getOrganizationId().equals(TenantContextHolder.getRequiredOrganizationId())) {
       throw new UnauthorizedOrganizationAccessException("Cross-tenant access forbidden");
   }
   ```
   Cross-tenant access attempts immediately terminate with an HTTP **403 Forbidden** and trigger a security audit log.
3. **Automated Auditing**: Every sensitive administrative, sequence, or messaging action writes to the `audit_logs` table with the actor's user ID, organization ID, and client IP.

---

## 3. Server-Side Request Forgery (SSRF) Protection

Prospecta enables users to input target company URLs for AI-driven intelligence gathering (`WebsiteResearchService`). To safeguard internal infrastructure against SSRF vulnerabilities, all candidate URLs pass through `SsrfValidator`:

```java
public static boolean isSafeUrl(String urlString) { ... }
```

### Protection Rules Enforced:
- **Protocol Restriction**: Only `http://` and `https://` schemes are permitted. Schemes like `file://`, `gopher://`, `ftp://` are rejected.
- **Localhost & Loopback Rejection**: Hostnames matching `localhost`, `127.0.0.1`, `::1` are blocked before DNS lookup.
- **DNS Resolution & IP Subnet Filter**:
  - `127.0.0.0/8` (Loopback)
  - `10.0.0.0/8` (Private RFC 1918)
  - `172.16.0.0/12` (Private RFC 1918)
  - `192.168.0.0/16` (Private RFC 1918)
  - `169.254.0.0/16` (Link-Local & Cloud Instance Metadata, e.g. AWS/GCP `169.254.169.254`)

---

## 4. Meta WhatsApp Webhook Security & Idempotency

### 4.1 HMAC-SHA256 Signature Verification
Meta Cloud API sends an `X-Hub-Signature-256` header on every inbound webhook POST.
`WhatsAppWebhookController` computes the expected signature using the shared secret (`prospecta.whatsapp.app-secret`):

$$\text{HMAC-SHA256}(\text{appSecret}, \text{rawPayload})$$

If the signature does not match, the request is immediately rejected with HTTP **401 Unauthorized**.

### 4.2 Webhook Idempotency & Deduplication
To handle network retries from Meta without duplicate processing, inbound messages are checked against the `external_events` table:

$$\text{Unique Constraint: } (provider, external\_event\_id)$$

If `wamid.XYZ` has already been recorded, the webhook returns HTTP **200 OK** immediately with status `DUPLICATE_EVENT_IGNORED`, preventing duplicate campaign state changes or customer replies.

---

## 5. Network Headers & CORS

Spring Security enforces:
- `X-Frame-Options: DENY` (Clickjacking prevention)
- `X-Content-Type-Options: nosniff` (MIME sniffing prevention)
- `Strict-Transport-Security: max-age=31536000; includeSubDomains` (HSTS)
- Strict CORS whitelist allowing only authorized frontends (`localhost:3000`, `localhost:5173`, or production domain).
