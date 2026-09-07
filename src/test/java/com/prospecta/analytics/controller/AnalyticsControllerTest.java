package com.prospecta.analytics.controller;

import com.prospecta.analytics.dto.AnalyticsDto.*;
import com.prospecta.analytics.service.AnalyticsService;
import com.prospecta.campaign.domain.CampaignStatus;
import com.prospecta.campaign.domain.ChannelType;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/analytics/overview - returns analytics overview")
    void shouldReturnAnalyticsOverview() throws Exception {
        AnalyticsOverviewResponse response = AnalyticsOverviewResponse.builder()
                .totalProspects(200)
                .qualifiedProspects(80)
                .contactedProspects(120)
                .repliedProspects(30)
                .meetingBookedProspects(10)
                .opportunitiesCount(25)
                .opportunitiesWon(8)
                .totalWonValue(BigDecimal.valueOf(24_000_000))
                .totalPipelineValue(BigDecimal.valueOf(75_000_000))
                .currency("XOF")
                .replyRate(25.0)
                .conversionRate(4.0)
                .activeCampaigns(3)
                .channelBreakdown(List.of(
                        ChannelMetricDto.builder().channel(ChannelType.WHATSAPP).totalSent(150).delivered(145).failed(5).build()
                ))
                .build();

        when(analyticsService.getOverview()).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProspects").value(200))
                .andExpect(jsonPath("$.data.qualifiedProspects").value(80))
                .andExpect(jsonPath("$.data.totalWonValue").value(24000000))
                .andExpect(jsonPath("$.data.currency").value("XOF"))
                .andExpect(jsonPath("$.data.replyRate").value(25.0));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/campaigns/{id} - returns campaign analytics")
    void shouldReturnCampaignAnalytics() throws Exception {
        UUID campaignId = UUID.randomUUID();
        CampaignAnalyticsResponse response = CampaignAnalyticsResponse.builder()
                .campaignId(campaignId)
                .campaignName("Campagne E-Commerce")
                .status(CampaignStatus.RUNNING)
                .targetProspectsCount(60)
                .repliedCount(12)
                .replyRate(20.0)
                .build();

        when(analyticsService.getCampaignAnalytics(campaignId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/campaigns/" + campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignId").value(campaignId.toString()))
                .andExpect(jsonPath("$.data.campaignName").value("Campagne E-Commerce"))
                .andExpect(jsonPath("$.data.targetProspectsCount").value(60))
                .andExpect(jsonPath("$.data.replyRate").value(20.0));
    }
}
