package com.prospecta.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.organization.domain.OrganizationPlan;
import com.prospecta.organization.domain.OrganizationStatus;
import com.prospecta.organization.dto.CreateOrganizationRequest;
import com.prospecta.organization.dto.OrganizationResponse;
import com.prospecta.organization.service.OrganizationService;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import com.prospecta.shared.exception.OrganizationNotFoundException;
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
class OrganizationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private OrganizationController organizationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(organizationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/organizations - should create organization and return 201 with standard response")
    void shouldCreateOrganization() throws Exception {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("Teranga Tech")
                .country("SN")
                .currency("XOF")
                .build();

        UUID orgId = UUID.randomUUID();
        OrganizationResponse response = OrganizationResponse.builder()
                .id(orgId)
                .name("Teranga Tech")
                .slug("teranga-tech")
                .country("SN")
                .currency("XOF")
                .status(OrganizationStatus.ACTIVE)
                .plan(OrganizationPlan.FREE)
                .createdAt(Instant.now())
                .build();

        when(organizationService.createOrganization(any(CreateOrganizationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(orgId.toString()))
                .andExpect(jsonPath("$.data.name").value("Teranga Tech"))
                .andExpect(jsonPath("$.data.slug").value("teranga-tech"))
                .andExpect(jsonPath("$.data.country").value("SN"))
                .andExpect(jsonPath("$.data.currency").value("XOF"));
    }

    @Test
    @DisplayName("POST /api/v1/organizations - should return 422 when validation fails (blank name)")
    void shouldReturn422WhenNameIsBlank() throws Exception {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .name("") // Blank name violates @NotBlank
                .build();

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/organizations/{id} - should return 404 when organization not found")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(organizationService.getOrganizationById(nonExistentId))
                .thenThrow(new OrganizationNotFoundException(nonExistentId));

        mockMvc.perform(get("/api/v1/organizations/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Organization not found with ID: " + nonExistentId));
    }
}
