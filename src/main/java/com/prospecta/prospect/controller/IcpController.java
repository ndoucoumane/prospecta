package com.prospecta.prospect.controller;

import com.prospecta.prospect.dto.IcpDto.CreateIcpRequest;
import com.prospecta.prospect.dto.IcpDto.IcpResponse;
import com.prospecta.prospect.dto.IcpDto.UpdateIcpRequest;
import com.prospecta.prospect.service.IcpService;
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
@RequestMapping("/api/v1/icp")
@RequiredArgsConstructor
@Tag(name = "Ideal Customer Profile", description = "Target ICP definition and criteria for scoring")
public class IcpController {

    private final IcpService icpService;

    @PostMapping
    @Operation(summary = "Create an Ideal Customer Profile (ICP)")
    public ResponseEntity<ApiResponse<IcpResponse>> createIcp(
            @Valid @RequestBody CreateIcpRequest request
    ) {
        IcpResponse response = icpService.createIcp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/active")
    @Operation(summary = "Get the active ICP for the organization")
    public ResponseEntity<ApiResponse<IcpResponse>> getActiveIcp() {
        IcpResponse response = icpService.getActiveIcp();
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ICP by ID")
    public ResponseEntity<ApiResponse<IcpResponse>> getIcpById(@PathVariable UUID id) {
        IcpResponse response = icpService.getIcpById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "List ICPs for the active organization")
    public ResponseEntity<ApiResponse<PageResponse<IcpResponse>>> getIcps(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<IcpResponse> response = icpService.getIcps(pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update ICP criteria")
    public ResponseEntity<ApiResponse<IcpResponse>> updateIcp(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIcpRequest request
    ) {
        IcpResponse response = icpService.updateIcp(id, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
