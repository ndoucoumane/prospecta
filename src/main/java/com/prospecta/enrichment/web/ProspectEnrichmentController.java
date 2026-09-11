package com.prospecta.enrichment.web;

import com.prospecta.enrichment.service.ProspectEnrichmentService;
import com.prospecta.prospect.dto.ProspectResponse;
import com.prospecta.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prospects")
@RequiredArgsConstructor
@Tag(name = "Enrichment", description = "Enrichissement des coordonnées (email, téléphone, mobile, profil entreprise)")
public class ProspectEnrichmentController {

    private final ProspectEnrichmentService prospectEnrichmentService;

    @PostMapping("/{id}/enrichment")
    @Operation(summary = "Enrichir un prospect existant via le fournisseur B2B (Apollo)")
    public ResponseEntity<ApiResponse<ProspectResponse>> enrichProspect(@PathVariable UUID id) {
        ProspectResponse response = prospectEnrichmentService.enrichProspect(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
