package com.prospecta.campaign.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.campaign.domain.CampaignStatus;
import com.prospecta.campaign.dto.CampaignDto.CampaignResponse;
import com.prospecta.campaign.dto.CampaignDto.CreateCampaignRequest;
import com.prospecta.campaign.service.CampaignService;
import com.prospecta.shared.exception.CampaignNotFoundException;
import com.prospecta.shared.exception.GlobalExceptionHandler;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController campaignController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(campaignController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/campaigns - should create campaign and return 201")
    void shouldCreateCampaign() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .name("Campagne Hôtels")
                .description("Offre digitale")
                .build();

        UUID campaignId = UUID.randomUUID();
        CampaignResponse response = CampaignResponse.builder()
                .id(campaignId)
                .name("Campagne Hôtels")
                .status(CampaignStatus.DRAFT)
                .steps(List.of())
                .build();

        when(campaignService.createCampaign(any(CreateCampaignRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(campaignId.toString()))
                .andExpect(jsonPath("$.data.name").value("Campagne Hôtels"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns/{id}/launch - should launch campaign and return RUNNING status")
    void shouldLaunchCampaign() throws Exception {
        UUID campaignId = UUID.randomUUID();
        CampaignResponse response = CampaignResponse.builder()
                .id(campaignId)
                .name("Campagne Hôtels")
                .status(CampaignStatus.RUNNING)
                .steps(List.of())
                .build();

        when(campaignService.launchCampaign(campaignId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/campaigns/" + campaignId + "/launch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(campaignId.toString()))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id} - should return 404 when campaign not found")
    void shouldReturn404WhenCampaignNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(campaignService.getCampaignById(nonExistentId))
                .thenThrow(new CampaignNotFoundException(nonExistentId));

        mockMvc.perform(get("/api/v1/campaigns/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CAMPAIGN_NOT_FOUND"));
    }
}
