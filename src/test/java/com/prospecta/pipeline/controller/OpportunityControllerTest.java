package com.prospecta.pipeline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.pipeline.domain.OpportunityStage;
import com.prospecta.pipeline.dto.OpportunityDto.*;
import com.prospecta.pipeline.service.PipelineService;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import com.prospecta.shared.exception.OpportunityNotFoundException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OpportunityControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PipelineService pipelineService;

    @InjectMocks
    private OpportunityController opportunityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(opportunityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/pipeline/opportunities - creates opportunity and returns 201")
    void shouldCreateOpportunity() throws Exception {
        UUID prospectId = UUID.randomUUID();
        UUID oppId = UUID.randomUUID();

        CreateOpportunityRequest request = CreateOpportunityRequest.builder()
                .prospectId(prospectId)
                .title("Contrat CRM Entreprise")
                .stage(OpportunityStage.QUALIFIED)
                .estimatedValue(BigDecimal.valueOf(5_000_000))
                .currency("XOF")
                .winProbability(40)
                .build();

        OpportunityResponse response = OpportunityResponse.builder()
                .id(oppId)
                .prospectId(prospectId)
                .prospectName("Mamadou Fall")
                .companyName("Sonatel")
                .title("Contrat CRM Entreprise")
                .stage(OpportunityStage.QUALIFIED)
                .estimatedValue(BigDecimal.valueOf(5_000_000))
                .currency("XOF")
                .winProbability(40)
                .createdAt(Instant.now())
                .build();

        when(pipelineService.createOpportunity(any(CreateOpportunityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/pipeline/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(oppId.toString()))
                .andExpect(jsonPath("$.data.title").value("Contrat CRM Entreprise"))
                .andExpect(jsonPath("$.data.stage").value("QUALIFIED"))
                .andExpect(jsonPath("$.data.estimatedValue").value(5000000))
                .andExpect(jsonPath("$.data.currency").value("XOF"));
    }

    @Test
    @DisplayName("PATCH /api/v1/pipeline/opportunities/{id}/stage - updates stage and returns 200")
    void shouldUpdateOpportunityStage() throws Exception {
        UUID oppId = UUID.randomUUID();
        UpdateOpportunityStageRequest request = UpdateOpportunityStageRequest.builder()
                .stage(OpportunityStage.WON)
                .build();

        OpportunityResponse response = OpportunityResponse.builder()
                .id(oppId)
                .title("Contrat CRM Entreprise")
                .stage(OpportunityStage.WON)
                .winProbability(100)
                .closedAt(Instant.now())
                .build();

        when(pipelineService.updateStage(eq(oppId), any(UpdateOpportunityStageRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/pipeline/opportunities/" + oppId + "/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("WON"))
                .andExpect(jsonPath("$.data.winProbability").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/pipeline/overview - returns pipeline stage breakdown")
    void shouldReturnPipelineOverview() throws Exception {
        PipelineOverviewResponse overview = PipelineOverviewResponse.builder()
                .totalOpportunities(12)
                .totalPipelineValue(BigDecimal.valueOf(45_000_000))
                .currency("XOF")
                .stages(List.of(
                        PipelineStageSummary.builder().stage(OpportunityStage.QUALIFIED).count(5).totalValue(BigDecimal.valueOf(15_000_000)).build(),
                        PipelineStageSummary.builder().stage(OpportunityStage.WON).count(3).totalValue(BigDecimal.valueOf(20_000_000)).build()
                ))
                .build();

        when(pipelineService.getPipelineOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/pipeline/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOpportunities").value(12))
                .andExpect(jsonPath("$.data.currency").value("XOF"))
                .andExpect(jsonPath("$.data.totalPipelineValue").value(45000000));
    }

    @Test
    @DisplayName("GET /api/v1/pipeline/opportunities/{id} - not found returns 404")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID oppId = UUID.randomUUID();
        when(pipelineService.getOpportunityById(oppId)).thenThrow(new OpportunityNotFoundException(oppId));

        mockMvc.perform(get("/api/v1/pipeline/opportunities/" + oppId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("OPPORTUNITY_NOT_FOUND"));
    }
}
