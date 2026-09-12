package com.prospecta.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Demande d'authentification utilisateur")
public class LoginRequest {

    @NotBlank(message = "L'adresse email ou nom d'utilisateur est obligatoire")
    @Schema(description = "Adresse email ou nom d'utilisateur Keycloak", example = "mamadou.diallo@prospecta.sn")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Schema(description = "Mot de passe", example = "Passer123!")
    private String password;
}
