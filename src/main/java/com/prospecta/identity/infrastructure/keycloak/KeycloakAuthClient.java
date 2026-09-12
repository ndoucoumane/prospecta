package com.prospecta.identity.infrastructure.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.identity.dto.AuthTokenResponse;
import com.prospecta.shared.exception.DuplicateResourceException;
import com.prospecta.shared.exception.InvalidCredentialsException;
import com.prospecta.shared.exception.KeycloakIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class KeycloakAuthClient {

    private final KeycloakProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KeycloakAuthClient(KeycloakProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.getAuthServerUrl())
                .defaultHeader("Cache-Control", "no-cache")
                .build();
    }

    /**
     * Authentifie l'utilisateur via Direct Access Grants (Password grant) et retourne les tokens JWT.
     */
    public AuthTokenResponse login(String usernameOrEmail, String password) {
        String tokenUri = String.format("/realms/%s/protocol/openid-connect/token", properties.getRealm());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.getClientId());
        String secret = properties.getEffectiveClientSecret();
        if (secret != null && !secret.isBlank()) {
            formData.add("client_secret", secret);
        }
        formData.add("username", usernameOrEmail);
        formData.add("password", password);
        formData.add("scope", "openid profile email");

        try {
            return restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(AuthTokenResponse.class);
        } catch (HttpClientErrorException ex) {
            log.warn("Keycloak login failed for user '{}' with status {}: {}", usernameOrEmail, ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new InvalidCredentialsException("Adresse email ou mot de passe incorrect");
            }
            throw new KeycloakIntegrationException("Erreur d'authentification Keycloak: " + ex.getStatusText(), HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (ResourceAccessException ex) {
            log.error("Unable to reach Keycloak server at {}: {}", properties.getAuthServerUrl(), ex.getMessage());
            throw new KeycloakIntegrationException("Le serveur d'authentification Keycloak est actuellement injoignable");
        } catch (Exception ex) {
            log.error("Unexpected error during Keycloak login: {}", ex.getMessage(), ex);
            throw new KeycloakIntegrationException("Erreur inattendue lors de la connexion via Keycloak");
        }
    }

    /**
     * Crée un utilisateur dans le realm Keycloak configuré et retourne son ID subject (UUID).
     */
    public String createUser(String firstName, String lastName, String email, String password) {
        String adminToken = obtainAdminAccessToken();

        String usersUri = String.format("/admin/realms/%s/users", properties.getRealm());

        Map<String, Object> credentialMap = new HashMap<>();
        credentialMap.put("type", "password");
        credentialMap.put("value", password);
        credentialMap.put("temporary", false);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", email.toLowerCase().trim());
        userPayload.put("email", email.toLowerCase().trim());
        userPayload.put("firstName", firstName != null ? firstName.trim() : "");
        userPayload.put("lastName", lastName != null ? lastName.trim() : "");
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", true);
        userPayload.put("credentials", Collections.singletonList(credentialMap));

        try {
            URI location = restClient.post()
                    .uri(usersUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();

            if (location != null) {
                String path = location.getPath();
                String keycloakSubject = path.substring(path.lastIndexOf('/') + 1);
                log.info("Created user in Keycloak realm '{}': email={}, keycloakSubject={}", properties.getRealm(), email, keycloakSubject);
                return keycloakSubject;
            }

            // Si le header Location n'est pas retourné, on recherche l'utilisateur par email
            return findUserIdByEmail(adminToken, email);

        } catch (HttpClientErrorException ex) {
            log.warn("Keycloak user creation failed for email '{}' [HTTP {}]: {}", email, ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new DuplicateResourceException("UserProfile", "email", email);
            }
            throw new KeycloakIntegrationException("Erreur lors de la création de l'utilisateur dans Keycloak: " + ex.getResponseBodyAsString(), HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (ResourceAccessException ex) {
            log.error("Unable to reach Keycloak server at {}: {}", properties.getAuthServerUrl(), ex.getMessage());
            throw new KeycloakIntegrationException("Le serveur d'authentification Keycloak est actuellement injoignable");
        } catch (Exception ex) {
            log.error("Unexpected error creating user in Keycloak: {}", ex.getMessage(), ex);
            throw new KeycloakIntegrationException("Erreur inattendue lors de la création de l'utilisateur dans Keycloak");
        }
    }

    /**
     * Recherche l'ID Keycloak d'un utilisateur par son email.
     */
    private String findUserIdByEmail(String adminToken, String email) {
        String searchUri = String.format("/admin/realms/%s/users?email=%s&exact=true", properties.getRealm(), email);
        try {
            String responseBody = restClient.get()
                    .uri(searchUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.isArray() && !root.isEmpty()) {
                return root.get(0).get("id").asText();
            }
        } catch (Exception e) {
            log.warn("Could not query user ID by email in Keycloak: {}", e.getMessage());
        }
        throw new KeycloakIntegrationException("L'utilisateur a été créé mais son identifiant Keycloak est introuvable");
    }

    /**
     * Obtient un token d'administration soit via Client Credentials, soit via le compte admin de secours.
     */
    private String obtainAdminAccessToken() {
        // 1. Essai avec Client Credentials grant sur le realm configuré
        String secret = properties.getEffectiveClientSecret();
        if (secret != null && !secret.isBlank()) {
            try {
                String tokenUri = String.format("/realms/%s/protocol/openid-connect/token", properties.getRealm());
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", "client_credentials");
                formData.add("client_id", properties.getClientId());
                formData.add("client_secret", secret);

                AuthTokenResponse tokenResponse = restClient.post()
                        .uri(tokenUri)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData)
                        .retrieve()
                        .body(AuthTokenResponse.class);

                if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                    return tokenResponse.getAccessToken();
                }
            } catch (Exception e) {
                log.debug("Client credentials grant not available or lacking admin roles ({}), falling back to admin account", e.getMessage());
            }
        }

        // 2. Repli sur le compte admin (realm master puis realm configuré)
        String[] realmsToTry = new String[]{properties.getAdminRealm() != null ? properties.getAdminRealm() : "master", properties.getRealm()};
        String[] usersToTry = new String[]{properties.getAdminUsername(), "morsolo777@gmail"};

        for (String realm : realmsToTry) {
            for (String username : usersToTry) {
                if (username == null || username.isBlank()) continue;
                try {
                    String tokenUri = String.format("/realms/%s/protocol/openid-connect/token", realm);
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add("grant_type", "password");
                    formData.add("client_id", "master".equals(realm) ? "admin-cli" : properties.getClientId());
                    if (!"master".equals(realm) && secret != null && !secret.isBlank()) {
                        formData.add("client_secret", secret);
                    }
                    formData.add("username", username);
                    formData.add("password", properties.getAdminPassword());

                    AuthTokenResponse tokenResponse = restClient.post()
                            .uri(tokenUri)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(formData)
                            .retrieve()
                            .body(AuthTokenResponse.class);

                    if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                        log.debug("Successfully obtained Keycloak admin token using user '{}' on realm '{}'", username, realm);
                        return tokenResponse.getAccessToken();
                    }
                } catch (Exception e) {
                    log.debug("Failed admin token attempt for realm '{}' user '{}': {}", realm, username, e.getMessage());
                }
            }
        }

        log.error("Failed to obtain Keycloak admin token with all configured methods");
        throw new KeycloakIntegrationException("Impossible d'obtenir un jeton d'administration Keycloak pour créer des utilisateurs. Vérifiez que Keycloak est bien démarré et que les identifiants admin sont valides.");
    }
}
