package com.prospecta.identity.infrastructure.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String realm = "clatous-production";
    private String authServerUrl = "https://auth.clatous.com";
    private String clientId = "clatous-backend-prod";
    private String clientSecret = "QWyJjXyZ7GPibKO21mw10lY3J2AzJlvqoU2IA6ODHxLlumIynJVPQxLzJolNYxRc2toMheLInzdcKYLeMStJKE";
    private String adminUsername = "keycloak-admin";
    private String adminPassword = "soloSaliou10@";
    private String adminRealm = "master";
    private Credentials credentials = new Credentials();

    @Getter
    @Setter
    public static class Credentials {
        private String secret;
    }

    public String getEffectiveClientSecret() {
        if (clientSecret != null && !clientSecret.isBlank()) {
            return clientSecret;
        }
        return credentials != null ? credentials.getSecret() : null;
    }
}
