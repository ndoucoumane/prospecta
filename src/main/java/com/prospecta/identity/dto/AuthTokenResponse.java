package com.prospecta.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Jetons d'authentification OIDC / JWT Keycloak")
public class AuthTokenResponse {

    @JsonProperty("access_token")
    @Schema(description = "Jeton d'accès JWT pour authentifier les requêtes API", example = "eyJhbGciOiJSUzI1NiIsInR5c...")
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(description = "Jeton de rafraîchissement", example = "eyJhbGciOiJIUzI1NiIsInR5c...")
    private String refreshToken;

    @JsonProperty("token_type")
    @Schema(description = "Type de jeton", example = "Bearer")
    private String tokenType;

    @JsonProperty("expires_in")
    @Schema(description = "Durée de validité en secondes", example = "300")
    private Long expiresIn;

    @JsonProperty("refresh_expires_in")
    @Schema(description = "Durée de validité du refresh token en secondes", example = "1800")
    private Long refreshExpiresIn;

    @JsonProperty("scope")
    @Schema(description = "Portée accordée", example = "openid profile email")
    private String scope;
}
