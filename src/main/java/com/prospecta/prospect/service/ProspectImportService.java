package com.prospecta.prospect.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.prospecta.prospect.domain.Company;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.dto.ImportProspectsReport;
import com.prospecta.prospect.dto.LeadScoreResult;
import com.prospecta.prospect.repository.CompanyRepository;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.shared.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProspectImportService {

    private final ProspectRepository prospectRepository;
    private final CompanyRepository companyRepository;
    private final DeduplicationService deduplicationService;
    private final LeadScoringEngine leadScoringEngine;
    private final AuditService auditService;

    @Transactional
    public ImportProspectsReport importCsv(MultipartFile file) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        int total = 0;
        int created = 0;
        int duplicates = 0;
        int invalid = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null || header.length == 0) {
                throw new IllegalArgumentException("CSV file has no headers");
            }

            Map<String, Integer> columnMap = buildColumnMap(header);

            String[] line;
            int lineNumber = 1;
            while ((line = csvReader.readNext()) != null) {
                lineNumber++;
                if (line.length == 0 || (line.length == 1 && line[0].isBlank())) {
                    continue; // Skip empty rows
                }
                total++;

                try {
                    String firstName = getValue(line, columnMap, "firstname", "prenom", "first_name");
                    String lastName = getValue(line, columnMap, "lastname", "nom", "last_name");
                    String email = getValue(line, columnMap, "email", "mail", "e-mail");
                    String rawPhone = getValue(line, columnMap, "phone", "telephone", "tel", "mobile");
                    String companyName = getValue(line, columnMap, "companyname", "company", "entreprise", "societe");
                    String companyWebsite = getValue(line, columnMap, "companywebsite", "website", "siteweb", "site");
                    String jobTitle = getValue(line, columnMap, "jobtitle", "job", "poste", "fonction", "title");
                    String city = getValue(line, columnMap, "city", "ville");
                    String industry = getValue(line, columnMap, "industry", "secteur", "activite");
                    String linkedinUrl = getValue(line, columnMap, "linkedinurl", "linkedin");

                    // Validation: must have at least email OR phone
                    if ((email == null || email.isBlank()) && (rawPhone == null || rawPhone.isBlank())) {
                        invalid++;
                        errors.add("Line " + lineNumber + ": Missing both email and phone number");
                        continue;
                    }

                    // E.164 phone normalization for Senegal (+221)
                    String normalizedPhone = null;
                    String whatsappNumber = null;
                    if (rawPhone != null && !rawPhone.isBlank()) {
                        Optional<String> e164 = PhoneNumberUtils.normalizeToE164(rawPhone, "SN");
                        if (e164.isPresent()) {
                            normalizedPhone = e164.get();
                            // In Senegal, mobile prefixes 70, 75, 76, 77, 78 are WhatsApp-ready
                            if (normalizedPhone.matches("^\\+221(70|75|76|77|78)\\d{7}$")) {
                                whatsappNumber = normalizedPhone;
                            }
                        } else {
                            // If phone is given but completely invalid, still store raw or mark invalid
                            normalizedPhone = rawPhone.trim();
                        }
                    }

                    // Deduplication check
                    Optional<Prospect> duplicate = deduplicationService.findDuplicate(
                            organizationId, email, normalizedPhone, whatsappNumber, firstName, lastName, companyName
                    );

                    if (duplicate.isPresent()) {
                        duplicates++;
                        continue;
                    }

                    // Company linkage / auto-creation
                    Company company = null;
                    if (companyName != null && !companyName.isBlank()) {
                        company = resolveOrCreateCompany(organizationId, companyName, companyWebsite, industry, city);
                    }

                    Prospect prospect = Prospect.builder()
                            .company(company)
                            .firstName(firstName)
                            .lastName(lastName)
                            .jobTitle(jobTitle)
                            .companyName(companyName)
                            .companyWebsite(companyWebsite)
                            .email(email != null && !email.isBlank() ? email.trim() : null)
                            .emailStatus(email != null && !email.isBlank() ? "VALID" : "UNKNOWN")
                            .phone(normalizedPhone)
                            .phoneStatus(normalizedPhone != null ? "VALID" : "UNKNOWN")
                            .whatsappNumber(whatsappNumber)
                            .country("SN")
                            .city(city != null && !city.isBlank() ? city : "Dakar")
                            .industry(industry)
                            .linkedinUrl(linkedinUrl)
                            .source("CSV_IMPORT")
                            .status(ProspectStatus.NEW)
                            .build();

                    prospect.setOrganizationId(organizationId);
                    prospect.setFullName(prospect.computeFullName());

                    // Lead Scoring
                    LeadScoreResult scoreResult = leadScoringEngine.score(prospect);
                    prospect.setLeadScore(scoreResult.getScore());
                    prospect.setLeadScoreLevel(scoreResult.getLevel());
                    prospect.setLeadScoreReasons(String.join("; ", scoreResult.getReasons()));

                    prospectRepository.save(prospect);
                    created++;

                } catch (Exception e) {
                    invalid++;
                    errors.add("Line " + lineNumber + ": Error processing row - " + e.getMessage());
                    log.warn("Error processing CSV row at line {}: {}", lineNumber, e.getMessage());
                }
            }

        } catch (IOException | CsvValidationException e) {
            log.error("Failed to read CSV file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage(), e);
        }

        auditService.logSync("PROSPECTS_IMPORTED", "Prospect", organizationId.toString(),
                String.format("Total: %d, Created: %d, Duplicates: %d, Invalid: %d", total, created, duplicates, invalid));

        log.info("Prospect import completed for org={}: total={}, created={}, duplicates={}, invalid={}",
                organizationId, total, created, duplicates, invalid);

        return ImportProspectsReport.builder()
                .total(total)
                .created(created)
                .duplicates(duplicates)
                .invalid(invalid)
                .errors(errors)
                .build();
    }

    private Company resolveOrCreateCompany(UUID organizationId, String name, String website, String industry, String city) {
        Optional<Company> existing = companyRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, name.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        Company company = Company.builder()
                .name(name.trim())
                .website(website)
                .industry(industry)
                .city(city)
                .country("SN")
                .source("CSV_IMPORT")
                .build();
        company.setOrganizationId(organizationId);
        return companyRepository.save(company);
    }

    private Map<String, Integer> buildColumnMap(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null) {
                String cleanHeader = header[i].trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                map.put(cleanHeader, i);
            }
        }
        return map;
    }

    private String getValue(String[] line, Map<String, Integer> map, String... possibleKeys) {
        for (String key : possibleKeys) {
            String cleanKey = key.toLowerCase().replaceAll("[^a-z0-9]", "");
            Integer idx = map.get(cleanKey);
            if (idx != null && idx < line.length && line[idx] != null && !line[idx].isBlank()) {
                return line[idx].trim();
            }
        }
        return null;
    }
}
