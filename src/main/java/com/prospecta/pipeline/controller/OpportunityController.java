package com.prospecta.pipeline.controller;

import com.prospecta.pipeline.domain.OpportunityStage;
import com.prospecta.pipeline.dto.OpportunityDto.*;
import com.prospecta.pipeline.service.PipelineService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
@Tag(name = "Pipeline", description = "Sales pipeline, deals, opportunities, and stage transitions")
public class OpportunityController {

    private final PipelineService pipelineService;

    @PostMapping("/opportunities")
    @Operation(summary = "Create an opportunity from a prospect")
    public ResponseEntity<ApiResponse<OpportunityResponse>> createOpportunity(
            @Valid @RequestBody CreateOpportunityRequest request
    ) {
        OpportunityResponse response = pipelineService.createOpportunity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/opportunities")
    @Operation(summary = "List opportunities (filtered by stage, assigned user, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<OpportunityResponse>>> getOpportunities(
            @RequestParam(required = false) OpportunityStage stage,
            @RequestParam(required = false) UUID assignedTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<OpportunityResponse> page = pipelineService.getOpportunities(stage, assignedTo, pageable);
        return ResponseEntity.ok(ApiResponse.of(page));
    }

    @GetMapping("/opportunities/{id}")
    @Operation(summary = "Get opportunity details by ID")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getOpportunityById(@PathVariable UUID id) {
        OpportunityResponse response = pipelineService.getOpportunityById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/opportunities/{id}/stage")
    @Operation(summary = "Update opportunity stage (e.g. advance to WON, LOST, MEETING_SCHEDULED)")
    public ResponseEntity<ApiResponse<OpportunityResponse>> updateStage(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOpportunityStageRequest request
    ) {
        OpportunityResponse response = pipelineService.updateStage(id, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/opportunities/{id}")
    @Operation(summary = "Update opportunity details")
    public ResponseEntity<ApiResponse<OpportunityResponse>> updateOpportunity(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOpportunityRequest request
    ) {
        OpportunityResponse response = pipelineService.updateOpportunity(id, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get sales pipeline overview and aggregates by stage (in XOF)")
    public ResponseEntity<ApiResponse<PipelineOverviewResponse>> getPipelineOverview() {
        PipelineOverviewResponse overview = pipelineService.getPipelineOverview();
        return ResponseEntity.ok(ApiResponse.of(overview));
    }
}
