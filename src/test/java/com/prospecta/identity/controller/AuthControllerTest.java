package com.prospecta.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.identity.domain.UserRole;
import com.prospecta.identity.domain.UserStatus;
import com.prospecta.identity.dto.*;
import com.prospecta.identity.service.AuthService;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import com.prospecta.shared.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - success should return 201 with AuthResponse")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Mamadou")
                .lastName("Diallo")
                .email("mamadou.diallo@prospecta.sn")
                .password("Password123!")
                .companyName("Diallo Tech")
                .phone("+221771234567")
                .build();

        AuthTokenResponse tokenResponse = AuthTokenResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(300L)
                .build();

        UserProfileResponse userProfile = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .keycloakSubject("kc-subject-123")
                .firstName("Mamadou")
                .lastName("Diallo")
                .fullName("Mamadou Diallo")
                .email("mamadou.diallo@prospecta.sn")
                .role(UserRole.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token(tokenResponse)
                .user(userProfile)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token.access_token").value("mock-access-token"))
                .andExpect(jsonPath("$.data.user.email").value("mamadou.diallo@prospecta.sn"))
                .andExpect(jsonPath("$.data.user.role").value("ORG_ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - invalid payload should return 422 Unprocessable Entity")
    void shouldFailValidationOnInvalidRegister() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("")
                .lastName("")
                .email("invalid-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - success should return 200 with AuthResponse")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("mamadou.diallo@prospecta.sn")
                .password("Password123!")
                .build();

        AuthTokenResponse tokenResponse = AuthTokenResponse.builder()
                .accessToken("mock-access-token-login")
                .refreshToken("mock-refresh-token-login")
                .tokenType("Bearer")
                .expiresIn(300L)
                .build();

        UserProfileResponse userProfile = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .keycloakSubject("kc-subject-123")
                .firstName("Mamadou")
                .lastName("Diallo")
                .fullName("Mamadou Diallo")
                .email("mamadou.diallo@prospecta.sn")
                .role(UserRole.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token(tokenResponse)
                .user(userProfile)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token.access_token").value("mock-access-token-login"))
                .andExpect(jsonPath("$.data.user.email").value("mamadou.diallo@prospecta.sn"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - invalid credentials should return 401 Unauthorized")
    void shouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("mamadou.diallo@prospecta.sn")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Adresse email ou mot de passe incorrect"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - should return current authenticated user profile")
    void shouldReturnCurrentUser() throws Exception {
        UserProfileResponse userProfile = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .keycloakSubject("kc-subject-123")
                .firstName("Mamadou")
                .lastName("Diallo")
                .fullName("Mamadou Diallo")
                .email("mamadou.diallo@prospecta.sn")
                .role(UserRole.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(authService.getCurrentUser()).thenReturn(userProfile);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("mamadou.diallo@prospecta.sn"))
                .andExpect(jsonPath("$.data.fullName").value("Mamadou Diallo"));
    }
}

