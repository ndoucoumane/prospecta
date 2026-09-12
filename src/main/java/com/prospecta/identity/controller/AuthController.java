package com.prospecta.identity.controller;

import com.prospecta.identity.dto.AuthResponse;
import com.prospecta.identity.dto.LoginRequest;
import com.prospecta.identity.dto.RegisterRequest;
import com.prospecta.identity.dto.UserProfileResponse;
import com.prospecta.identity.service.AuthService;
import com.prospecta.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints d'inscription, de connexion et de session utilisateur Keycloak")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Inscrire un nouvel utilisateur et initialiser son organisation", description = "Crée le compte dans Keycloak (realm clatous-production), configure l'espace de travail Prospecta et délivre les jetons JWT.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Connecter un utilisateur existant", description = "Vérifie les identifiants auprès de Keycloak et retourne les jetons JWT (access_token, refresh_token) ainsi que le profil utilisateur.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Keycloak-JWT")
    @Operation(summary = "Récupérer les informations de l'utilisateur connecté", description = "Retourne le profil et l'organisation de l'utilisateur associé au jeton Bearer JWT fourni.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
        UserProfileResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
