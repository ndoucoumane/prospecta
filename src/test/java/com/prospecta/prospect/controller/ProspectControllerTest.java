package com.prospecta.prospect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.prospect.domain.LeadScoreLevel;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.CreateProspectRequest;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.dto.ProspectResponse;
import com.prospecta.prospect.service.ProspectImportService;
import com.prospecta.prospect.service.ProspectService;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import com.prospecta.shared.exception.ProspectNotFoundException;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProspectControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProspectService prospectService;

    @Mock
    private ProspectImportService prospectImportService;

    @InjectMocks
    private ProspectController prospectController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(prospectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/prospects - should create prospect and return 201 with score")
    void shouldCreateProspect() throws Exception {
        CreateProspectRequest request = CreateProspectRequest.builder()
                .firstName("Moussa")
                .lastName("Fall")
                .email("moussa.fall@hotel.sn")
                .phone("+221771234567")
                .companyName("Hotel Teranga")
                .jobTitle("Directeur Commercial")
                .city("Dakar")
                .build();

        UUID prospectId = UUID.randomUUID();
        ProspectResponse response = ProspectResponse.builder()
                .id(prospectId)
                .firstName("Moussa")
                .lastName("Fall")
                .fullName("Moussa Fall")
                .email("moussa.fall@hotel.sn")
                .phone("+221771234567")
                .whatsappNumber("+221771234567")
                .status(ProspectStatus.NEW)
                .leadScore(85)
                .leadScoreLevel(LeadScoreLevel.VERY_HIGH)
                .leadScoreReasons("Dakar; WhatsApp; Décideur")
                .createdAt(Instant.now())
                .build();

        when(prospectService.createProspect(any(CreateProspectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/prospects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(prospectId.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Moussa Fall"))
                .andExpect(jsonPath("$.data.phone").value("+221771234567"))
                .andExpect(jsonPath("$.data.leadScore").value(85))
                .andExpect(jsonPath("$.data.leadScoreLevel").value("VERY_HIGH"));
    }

    @Test
    @DisplayName("GET /api/v1/prospects/{id} - should return 404 with PROSPECT_NOT_FOUND when prospect not found")
    void shouldReturn404WhenProspectNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(prospectService.getProspectById(nonExistentId))
                .thenThrow(new ProspectNotFoundException(nonExistentId));

        mockMvc.perform(get("/api/v1/prospects/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROSPECT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Prospect not found with ID: " + nonExistentId));
    }

    @Test
    @DisplayName("POST /api/v1/prospects/{id}/score - should recalculate and return lead score")
    void shouldScoreProspect() throws Exception {
        UUID prospectId = UUID.randomUUID();
        LeadScoreResult result = LeadScoreResult.builder()
                .score(90)
                .level(LeadScoreLevel.VERY_HIGH)
                .reasons(List.of("Dakar", "WhatsApp", "Décideur"))
                .build();

        when(prospectService.scoreProspect(prospectId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(90))
                .andExpect(jsonPath("$.data.level").value("VERY_HIGH"))
                .andExpect(jsonPath("$.data.reasons").isArray());
    }
}
