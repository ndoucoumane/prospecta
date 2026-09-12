package com.prospecta.analytics.controller;

import com.prospecta.analytics.dto.AnalyticsDto.*;
import com.prospecta.analytics.service.AnalyticsService;
import com.prospecta.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


//Controller
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Tenant sales performance, campaign tracking, reply rates, and conversion metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get high-level sales and engagement metrics overview for active organization")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview() {
        AnalyticsOverviewResponse overview = analyticsService.getOverview();
        return ResponseEntity.ok(ApiResponse.of(overview));
    }

    @GetMapping("/campaigns/{id}")
    @Operation(summary = "Get detailed analytics and conversion funnel for a specific campaign")
    public ResponseEntity<ApiResponse<CampaignAnalyticsResponse>> getCampaignAnalytics(@PathVariable UUID id) {
        CampaignAnalyticsResponse response = analyticsService.getCampaignAnalytics(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
