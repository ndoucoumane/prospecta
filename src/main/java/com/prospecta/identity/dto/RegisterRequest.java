package com.prospecta.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Demande d'inscription d'un nouvel utilisateur et création de son organisation")
public class RegisterRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit comporter entre 2 et 100 caractères")
    @Schema(description = "Prénom de l'utilisateur", example = "Mamadou")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit comporter entre 2 et 100 caractères")
    @Schema(description = "Nom de l'utilisateur", example = "Diallo")
    private String lastName;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email doit être valide")
    @Schema(description = "Adresse email professionnelle", example = "mamadou.diallo@prospecta.sn")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit comporter au moins 8 caractères")
    @Schema(description = "Mot de passe sécurisé (min 8 caractères)", example = "Passer123!")
    private String password;

    @Schema(description = "Nom de l'entreprise ou de l'organisation (facultatif)", example = "Diallo Consulting")
    private String companyName;

    @Schema(description = "Numéro de téléphone (facultatif)", example = "+221771234567")
    private String phone;
}
