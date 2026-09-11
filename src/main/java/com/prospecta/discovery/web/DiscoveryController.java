package com.prospecta.discovery.web;

import com.prospecta.discovery.application.CompanyDiscoveryService;
import com.prospecta.discovery.application.DiscoveryImportService;
import com.prospecta.discovery.application.ProspectDiscoveryService;
import com.prospecta.discovery.domain.CompanySearchRequest;
import com.prospecta.discovery.domain.CompanySearchResult;
import com.prospecta.discovery.domain.DiscoveredCompany;
import com.prospecta.discovery.domain.PeopleSearchRequest;
import com.prospecta.discovery.domain.PeopleSearchResult;
import com.prospecta.discovery.web.dto.ImportPeopleReport;
import com.prospecta.discovery.web.dto.ImportPeopleRequest;
import com.prospecta.prospect.domain.Company;
import com.prospecta.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
@Tag(name = "Discovery", description = "B2B Prospect & Company search and selective import (Apollo provider)")
public class DiscoveryController {

    private final ProspectDiscoveryService prospectDiscoveryService;
    private final CompanyDiscoveryService companyDiscoveryService;
    private final DiscoveryImportService discoveryImportService;

    @PostMapping("/people/search")
    @Operation(summary = "Rechercher des personnes / prospects avec filtres B2B (Apollo)")
    public ResponseEntity<ApiResponse<PeopleSearchResult>> searchPeople(
            @Valid @RequestBody PeopleSearchRequest request
    ) {
        PeopleSearchResult result = prospectDiscoveryService.searchPeople(request);
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    @PostMapping("/companies/search")
    @Operation(summary = "Rechercher des entreprises avec filtres B2B (Apollo)")
    public ResponseEntity<ApiResponse<CompanySearchResult>> searchCompanies(
            @Valid @RequestBody CompanySearchRequest request
    ) {
        CompanySearchResult result = companyDiscoveryService.searchCompanies(request);
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    @PostMapping("/people/import")
    @Operation(summary = "Importer des prospects sélectionnés vers la base Prospecta et une Lead List")
    public ResponseEntity<ApiResponse<ImportPeopleReport>> importPeople(
            @RequestBody ImportPeopleRequest request
    ) {
        ImportPeopleReport report = discoveryImportService.importPeople(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(report));
    }

    @PostMapping("/companies/import")
    @Operation(summary = "Importer une entreprise sélectionnée vers la base Prospecta")
    public ResponseEntity<ApiResponse<Company>> importCompany(
            @RequestBody DiscoveredCompany request
    ) {
        Company saved = discoveryImportService.importCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(saved));
    }
}
