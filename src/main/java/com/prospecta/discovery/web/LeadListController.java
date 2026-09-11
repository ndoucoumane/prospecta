package com.prospecta.discovery.web;

import com.prospecta.discovery.application.LeadListService;
import com.prospecta.discovery.infrastructure.persistence.LeadList;
import com.prospecta.discovery.web.dto.CreateLeadListRequest;
import com.prospecta.discovery.web.dto.LeadListResponse;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.dto.ProspectResponse;
import com.prospecta.shared.dto.ApiResponse;
import com.prospecta.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lead-lists")
@RequiredArgsConstructor
@Tag(name = "Lead Lists", description = "Gestion des listes de prospects issues de la prospection ou découverte")
public class LeadListController {

    private final LeadListService leadListService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle liste de prospects")
    public ResponseEntity<ApiResponse<LeadListResponse>> createLeadList(
            @Valid @RequestBody CreateLeadListRequest request
    ) {
        LeadList list = leadListService.createLeadList(request.name(), request.description());
        LeadListResponse response = LeadListResponse.fromEntity(list, 0L);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "Lister les listes de prospects de l'organisation (paginé)")
    public ResponseEntity<ApiResponse<PageResponse<LeadListResponse>>> getLeadLists(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<LeadList> page = leadListService.getLeadLists(pageable);
        Page<LeadListResponse> dtoPage = page.map(list ->
                LeadListResponse.fromEntity(list, leadListService.countMembers(list.getId())));

        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(dtoPage)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une liste de prospects")
    public ResponseEntity<ApiResponse<LeadListResponse>> getLeadListById(@PathVariable UUID id) {
        LeadList list = leadListService.getLeadList(id);
        long count = leadListService.countMembers(id);
        return ResponseEntity.ok(ApiResponse.of(LeadListResponse.fromEntity(list, count)));
    }

    @GetMapping("/{id}/prospects")
    @Operation(summary = "Obtenir les prospects associés à une liste (paginé)")
    public ResponseEntity<ApiResponse<PageResponse<ProspectResponse>>> getProspectsInList(
            @PathVariable UUID id,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Prospect> page = leadListService.getProspectsInList(id, pageable);
        Page<ProspectResponse> dtoPage = page.map(ProspectResponse::from);

        return ResponseEntity.ok(ApiResponse.of(PageResponse.from(dtoPage)));
    }

    @PostMapping("/{id}/prospects")
    @Operation(summary = "Ajouter des prospects existants à une liste")
    public ResponseEntity<Void> addProspectsToList(
            @PathVariable UUID id,
            @RequestBody List<UUID> prospectIds
    ) {
        leadListService.addProspectsToList(id, prospectIds);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/prospects/{prospectId}")
    @Operation(summary = "Retirer un prospect d'une liste")
    public ResponseEntity<Void> removeProspectFromList(
            @PathVariable UUID id,
            @PathVariable UUID prospectId
    ) {
        leadListService.removeProspectFromList(id, prospectId);
        return ResponseEntity.noContent().build();
    }
}
