package com.prospecta.identity.dto;

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
@Schema(description = "Réponse d'authentification complète avec tokens et profil utilisateur")
public class AuthResponse {

    @Schema(description = "Tokens d'authentification Keycloak")
    private AuthTokenResponse token;

    @Schema(description = "Profil de l'utilisateur connecté")
    private UserProfileResponse user;
}
