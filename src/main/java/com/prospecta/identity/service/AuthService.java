package com.prospecta.identity.service;

import com.prospecta.identity.domain.UserProfile;
import com.prospecta.identity.domain.UserRole;
import com.prospecta.identity.domain.UserStatus;
import com.prospecta.identity.dto.*;
import com.prospecta.identity.infrastructure.keycloak.KeycloakAuthClient;
import com.prospecta.identity.repository.UserProfileRepository;
import com.prospecta.organization.domain.Organization;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.domain.OrganizationStatus;
import com.prospecta.organization.repository.OrganizationRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.UserProfileNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAuthClient keycloakAuthClient;
    private final UserProfileRepository userProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final UserProfileService userProfileService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Enregistre un nouvel utilisateur :
     * 1. Vérification d'unicité locale
     * 2. Création dans Keycloak (realm clatous-production)
     * 3. Création de son organisation (Workspace)
     * 4. Création de son profil administrateur d'organisation
     * 5. Connexion automatique auprès de Keycloak pour retourner les tokens JWT
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userProfileRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("UserProfile", "email", normalizedEmail);
        }

        // 1. Création dans Keycloak
        String keycloakSubject = keycloakAuthClient.createUser(
                request.getFirstName(),
                request.getLastName(),
                normalizedEmail,
                request.getPassword()
        );

        // 2. Création de l'Organisation par défaut
        String companyName = (request.getCompanyName() != null && !request.getCompanyName().isBlank())
                ? request.getCompanyName().trim()
                : request.getFirstName().trim() + " Workspace";

        String slug = generateUniqueSlug(companyName);

        Organization organization = Organization.builder()
                .name(companyName)
                .slug(slug)
                .country("SN")
                .timezone("Africa/Dakar")
                .currency("XOF")
                .status(OrganizationStatus.ACTIVE)
                .plan(OrganizationPlan.FREE)
                .email(normalizedEmail)
                .phone(request.getPhone())
                .build();

        Organization savedOrg = organizationRepository.save(organization);
        log.info("Created default organization '{}' [id={}] for user '{}'", savedOrg.getName(), savedOrg.getId(), normalizedEmail);

        // 3. Création du UserProfile
        UserProfile profile = UserProfile.builder()
                .organization(savedOrg)
                .keycloakSubject(keycloakSubject)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .phone(request.getPhone())
                .role(UserRole.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Provisioned user profile [id={}] linked to Keycloak subject '{}'", savedProfile.getId(), keycloakSubject);

        auditService.logSync("USER_REGISTERED", "UserProfile", savedProfile.getId().toString(), "Inscription de l'utilisateur: " + normalizedEmail);

        // 4. Authentification directe auprès de Keycloak pour délivrer le token
        AuthTokenResponse tokenResponse = keycloakAuthClient.login(normalizedEmail, request.getPassword());

        return AuthResponse.builder()
                .token(tokenResponse)
                .user(UserProfileResponse.from(savedProfile))
                .build();
    }

    /**
     * Authentifie l'utilisateur auprès de Keycloak et retourne les tokens ainsi que son profil local.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmail().trim();

        // 1. Authentification via Keycloak
        AuthTokenResponse tokenResponse = keycloakAuthClient.login(identifier, request.getPassword());

        // 2. Récupération ou initialisation du profil utilisateur local
        String normalizedEmail = identifier.toLowerCase();
        Optional<UserProfile> profileOpt = userProfileRepository.findByEmail(normalizedEmail);

        UserProfile profile = profileOpt.orElseGet(() -> {
            log.info("User logged in via Keycloak without local profile, auto-provisioning for: {}", normalizedEmail);
            String baseSlug = slugify(normalizedEmail.split("@")[0]);
            String slug = baseSlug;
            int counter = 1;
            while (organizationRepository.existsBySlug(slug)) {
                slug = baseSlug + "-" + counter++;
            }

            Organization defaultOrg = organizationRepository.save(Organization.builder()
                    .name(normalizedEmail.split("@")[0] + " Workspace")
                    .slug(slug)
                    .country("SN")
                    .timezone("Africa/Dakar")
                    .currency("XOF")
                    .status(OrganizationStatus.ACTIVE)
                    .plan(OrganizationPlan.FREE)
                    .email(normalizedEmail)
                    .build());

            return userProfileRepository.save(UserProfile.builder()
                    .organization(defaultOrg)
                    .keycloakSubject(UUID.randomUUID().toString())
                    .email(normalizedEmail)
                    .firstName("")
                    .lastName("")
                    .role(UserRole.ORG_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        auditService.logSync("USER_LOGGED_IN", "UserProfile", profile.getId().toString(), "Connexion réussie pour: " + normalizedEmail);

        return AuthResponse.builder()
                .token(tokenResponse)
                .user(UserProfileResponse.from(profile))
                .build();
    }

    /**
     * Retourne le profil de l'utilisateur actuellement authentifié via son Bearer Token.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        Optional<UserPrincipal> principalOpt = TenantContextHolder.getUserPrincipal();
        if (principalOpt.isPresent()) {
            UserPrincipal principal = principalOpt.get();
            if (principal.getKeycloakSubject() != null) {
                return userProfileRepository.findByKeycloakSubjectWithOrganization(principal.getKeycloakSubject())
                        .map(UserProfileResponse::from)
                        .orElseGet(userProfileService::getCurrentUserProfile);
            }
            if (principal.getEmail() != null) {
                return userProfileRepository.findByEmail(principal.getEmail())
                        .map(UserProfileResponse::from)
                        .orElseGet(userProfileService::getCurrentUserProfile);
            }
        }
        return userProfileService.getCurrentUserProfile();
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = slugify(name);
        String candidate = baseSlug.isBlank() ? "workspace" : baseSlug;
        String slug = candidate;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = candidate + "-" + counter++;
        }
        return slug;
    }

    private String slugify(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
