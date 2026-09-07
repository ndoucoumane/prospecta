package com.prospecta.campaign.controller;

import com.prospecta.campaign.dto.CampaignDto.*;
import com.prospecta.campaign.service.CampaignService;
import com.prospecta.shared.dto.ApiResponse;
import com.prospecta.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "Campaign definition, sequence steps, target prospects, and orchestration")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @Operation(summary = "Create a new campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID (tenant isolated)")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable UUID id) {
        CampaignResponse response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "List campaigns for the active organization (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<CampaignResponse>>> getCampaigns(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CampaignResponse> response = campaignService.getCampaigns(pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/steps")
    @Operation(summary = "Configure or update sequence steps for a campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> configureSteps(
            @PathVariable UUID id,
            @Valid @RequestBody List<CampaignStepRequest> stepRequests
    ) {
        CampaignResponse response = campaignService.configureSteps(id, stepRequests);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/prospects")
    @Operation(summary = "Attach target prospects to a campaign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addProspects(
            @PathVariable UUID id,
            @Valid @RequestBody AddProspectsRequest request
    ) {
        int added = campaignService.addProspects(id, request);
        return ResponseEntity.ok(ApiResponse.of(Map.of("addedCount", added, "campaignId", id)));
    }

    @PostMapping("/{id}/launch")
    @Operation(summary = "Launch a campaign: activates target sequences and dispatches initial messages")
    public ResponseEntity<ApiResponse<CampaignResponse>> launchCampaign(@PathVariable UUID id) {
        CampaignResponse response = campaignService.launchCampaign(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause an active campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> pauseCampaign(@PathVariable UUID id) {
        CampaignResponse response = campaignService.pauseCampaign(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
