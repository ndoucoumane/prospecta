package com.prospecta.prospect.controller;

import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.*;
import com.prospecta.prospect.service.ProspectImportService;
import com.prospecta.prospect.service.ProspectService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prospects")
@RequiredArgsConstructor
@Tag(name = "Prospects", description = "Prospect management, CSV imports, and lead scoring")
public class ProspectController {

    private final ProspectService prospectService;
    private final ProspectImportService prospectImportService;

    @PostMapping
    @Operation(summary = "Create a new prospect with automatic phone normalization and lead scoring")
    public ResponseEntity<ApiResponse<ProspectResponse>> createProspect(
            @Valid @RequestBody CreateProspectRequest request
    ) {
        ProspectResponse response = prospectService.createProspect(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a prospect by ID (tenant isolated)")
    public ResponseEntity<ApiResponse<ProspectResponse>> getProspectById(
            @PathVariable UUID id
    ) {
        ProspectResponse response = prospectService.getProspectById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "Search and filter prospects (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ProspectResponse>>> searchProspects(
            @RequestParam(required = false) ProspectStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProspectResponse> response = prospectService.searchProspects(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update prospect attributes")
    public ResponseEntity<ApiResponse<ProspectResponse>> updateProspect(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProspectRequest request
    ) {
        ProspectResponse response = prospectService.updateProspect(id, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prospect (data privacy / compliance)")
    public ResponseEntity<Void> deleteProspect(@PathVariable UUID id) {
        prospectService.deleteProspect(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/score")
    @Operation(summary = "Trigger lead scoring recalculation for a prospect")
    public ResponseEntity<ApiResponse<LeadScoreResult>> scoreProspect(@PathVariable UUID id) {
        LeadScoreResult result = prospectService.scoreProspect(id);
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import prospects from CSV file with normalization, deduplication, and scoring")
    public ResponseEntity<ApiResponse<ImportProspectsReport>> importProspects(
            @RequestParam("file") MultipartFile file
    ) {
        ImportProspectsReport report = prospectImportService.importCsv(file);
        return ResponseEntity.ok(ApiResponse.of(report));
    }
}
