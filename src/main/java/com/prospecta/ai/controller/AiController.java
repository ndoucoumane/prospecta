package com.prospecta.ai.controller;

import com.prospecta.ai.dto.AiDto.AiUsageSummary;
import com.prospecta.ai.dto.AiDto.CompanyAnalysisResult;
import com.prospecta.ai.dto.AiDto.GenerateMessageRequest;
import com.prospecta.ai.dto.AiDto.GenerateMessageResult;
import com.prospecta.ai.dto.AiDto.ProspectSummaryResult;
import com.prospecta.ai.service.CompanyAnalysisService;
import com.prospecta.ai.service.MessageGenerationService;
import com.prospecta.ai.service.ProspectSummaryService;
import com.prospecta.ai.service.QuotaService;
import com.prospecta.shared.dto.ApiResponse;
import com.prospecta.shared.security.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI Intelligence", description = "AI services: Company analysis, Prospect summaries, and copy generation")
public class AiController {

    private final CompanyAnalysisService companyAnalysisService;
    private final ProspectSummaryService prospectSummaryService;
    private final MessageGenerationService messageGenerationService;
    private final QuotaService quotaService;

    @PostMapping("/api/v1/companies/{id}/ai/analyze")
    @Operation(summary = "Analyze company web intelligence and extract structured insights via AI")
    public ResponseEntity<ApiResponse<CompanyAnalysisResult>> analyzeCompany(@PathVariable UUID id) {
        CompanyAnalysisResult result = companyAnalysisService.analyzeCompany(id);
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    @PostMapping("/api/v1/prospects/{id}/ai/summarize")
    @Operation(summary = "Generate a concise B2B summary and angle of approach for a prospect")
    public ResponseEntity<ApiResponse<ProspectSummaryResult>> summarizeProspect(@PathVariable UUID id) {
        ProspectSummaryResult result = prospectSummaryService.summarizeProspect(id);
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    @PostMapping("/api/v1/ai/messages/generate")
    @Operation(summary = "Generate a personalized sales outreach message for WhatsApp or Email")
    public ResponseEntity<ApiResponse<GenerateMessageResult>> generateMessage(
            @Valid @RequestBody GenerateMessageRequest request
    ) {
        GenerateMessageResult result = messageGenerationService.generateMessage(request);
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    @GetMapping("/api/v1/ai/usage")
    @Operation(summary = "Get current organization AI operations quota and token usage")
    public ResponseEntity<ApiResponse<AiUsageSummary>> getUsageSummary() {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        AiUsageSummary summary = quotaService.getUsageSummary(organizationId);
        return ResponseEntity.ok(ApiResponse.of(summary));
    }
}
