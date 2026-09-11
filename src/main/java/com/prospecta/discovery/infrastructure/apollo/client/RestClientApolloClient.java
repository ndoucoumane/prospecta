package com.prospecta.discovery.infrastructure.apollo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.discovery.domain.DiscoveryErrorCode;
import com.prospecta.discovery.domain.DiscoveryException;
import com.prospecta.discovery.infrastructure.apollo.config.ApolloProperties;
import com.prospecta.discovery.infrastructure.apollo.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RestClientApolloClient implements ApolloClient {

    private final ApolloProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestClientApolloClient(ApolloProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Cache-Control", "no-cache")
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    int statusCode = resp.getStatusCode().value();
                    if (statusCode == 401 || statusCode == 403) {
                        log.warn("Apollo authentication error [HTTP {}]", statusCode);
                        throw new DiscoveryException(DiscoveryErrorCode.AUTHENTICATION_ERROR, "Clé API Apollo invalide ou non autorisée");
                    } else if (statusCode == 429) {
                        log.warn("Apollo rate limit reached [HTTP 429]");
                        throw new DiscoveryException(DiscoveryErrorCode.RATE_LIMITED, "Limite d'appels API Apollo atteinte");
                    } else {
                        log.warn("Apollo bad request [HTTP {}]", statusCode);
                        throw new DiscoveryException(DiscoveryErrorCode.INVALID_REQUEST, "Critères de recherche Apollo invalides (HTTP " + statusCode + ")");
                    }
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    log.error("Apollo server error [HTTP {}]", resp.getStatusCode().value());
                    throw new DiscoveryException(DiscoveryErrorCode.PROVIDER_UNAVAILABLE, "Le service Apollo est temporairement indisponible (HTTP " + resp.getStatusCode().value() + ")");
                })
                .build();
    }

    @Override
    public ApolloPeopleSearchResponse searchPeople(ApolloPeopleSearchRequest request) {
        if (!properties.isConfigured()) {
            log.info("Clé API Apollo non configurée. Génération de résultats de recherche simulés pour le développement local.");
            return generateMockPeopleSearchResponse(request);
        }

        try {
            log.debug("Appel Apollo /api/v1/mixed_people/api_search (page={}, per_page={})", request.getPage(), request.getPerPage());
            return restClient.post()
                    .uri("/api/v1/mixed_people/api_search")
                    .header("x-api-key", properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(ApolloPeopleSearchResponse.class);
        } catch (DiscoveryException de) {
            throw de;
        } catch (ResourceAccessException rae) {
            log.error("Timeout ou erreur réseau lors de l'appel Apollo: {}", rae.getMessage());
            throw new DiscoveryException(DiscoveryErrorCode.TIMEOUT, "Délai d'attente dépassé lors de l'appel Apollo", rae);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'appel Apollo: {}", e.getMessage(), e);
            throw new DiscoveryException(DiscoveryErrorCode.PROVIDER_ERROR, "Erreur inattendue lors de la communication avec Apollo", e);
        }
    }

    @Override
    public ApolloOrgSearchResponse searchOrganizations(ApolloOrgSearchRequest request) {
        if (!properties.isConfigured()) {
            log.info("Clé API Apollo non configurée. Génération de résultats entreprises simulés pour le développement local.");
            return generateMockOrgSearchResponse(request);
        }

        try {
            log.debug("Appel Apollo /api/v1/mixed_companies/search (page={}, per_page={})", request.getPage(), request.getPerPage());
            return restClient.post()
                    .uri("/api/v1/mixed_companies/search")
                    .header("x-api-key", properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(ApolloOrgSearchResponse.class);
        } catch (DiscoveryException de) {
            throw de;
        } catch (ResourceAccessException rae) {
            log.error("Timeout ou erreur réseau lors de l'appel Apollo Companies: {}", rae.getMessage());
            throw new DiscoveryException(DiscoveryErrorCode.TIMEOUT, "Délai d'attente dépassé lors de l'appel Apollo Companies", rae);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'appel Apollo Companies: {}", e.getMessage(), e);
            throw new DiscoveryException(DiscoveryErrorCode.PROVIDER_ERROR, "Erreur inattendue lors de la communication avec Apollo", e);
        }
    }

    @Override
    public ApolloPersonMatchResponse matchPerson(ApolloPersonMatchRequest request) {
        if (!properties.isConfigured()) {
            log.info("Clé API Apollo non configurée. Simulation de l'enrichissement Apollo pour le prospect {}", request.getName());
            return generateMockPersonMatchResponse(request);
        }

        try {
            log.debug("Appel Apollo /api/v1/people/match pour prospect {}", request.getName());
            return restClient.post()
                    .uri("/api/v1/people/match")
                    .header("x-api-key", properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(ApolloPersonMatchResponse.class);
        } catch (DiscoveryException de) {
            throw de;
        } catch (ResourceAccessException rae) {
            log.error("Timeout ou erreur réseau lors de l'enrichissement Apollo: {}", rae.getMessage());
            throw new DiscoveryException(DiscoveryErrorCode.TIMEOUT, "Délai d'attente dépassé lors de l'enrichissement Apollo", rae);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'enrichissement Apollo: {}", e.getMessage(), e);
            throw new DiscoveryException(DiscoveryErrorCode.PROVIDER_ERROR, "Erreur inattendue lors de l'enrichissement Apollo", e);
        }
    }

    // =========================================================================
    // DONNÉES SIMULÉES LOCALES (DÉVELOPPEMENT & DÉMO SANS CLÉ PAYANTE)
    // =========================================================================

    private ApolloPeopleSearchResponse generateMockPeopleSearchResponse(ApolloPeopleSearchRequest req) {
        String job = (req.getPersonTitles() != null && !req.getPersonTitles().isEmpty()) ? req.getPersonTitles().get(0) : "Directeur commercial";
        String loc = (req.getPersonLocations() != null && !req.getPersonLocations().isEmpty()) ? req.getPersonLocations().get(0) : "Dakar, SN";
        String country = loc.contains("SN") || loc.toLowerCase().contains("sénégal") ? "Senegal" : "Senegal";
        String city = loc.contains("Dakar") ? "Dakar" : "Dakar";

        List<ApolloPerson> people = new ArrayList<>();
        String[][] mockPeopleData = {
                {"Mamadou", "Diop", job, "Gainde 2000", "gainde2000.sn", "Technology", "150", "mamadou.diop@gainde2000.sn", "+221776543210"},
                {"Fatou", "Ndiaye", "Directrice Générale", "Wave Mobile Money", "wave.com", "Fintech", "350", "fatou.ndiaye@wave.com", "+221781234567"},
                {"Cheikh", "Ba", job, "Atos Sénégal", "atos.net", "IT Services", "200", "cheikh.ba@atos.net", "+221762345678"},
                {"Awa", "Fall", "Head of Sales", "Sonatel Orange", "orange.sn", "Telecommunications", "2000", "awa.fall@orange.sn", "+221773456789"},
                {"Ousmane", "Sow", "Chief Technology Officer", "Free Sénégal", "free.sn", "Telecommunications", "800", "ousmane.sow@free.sn", "+221704567890"},
                {"Mariama", "Diallo", job, "Touchpoint SN", "touchpoint.sn", "Software", "45", "mariama.diallo@touchpoint.sn", "+221775678901"},
                {"Ibrahima", "Cissé", "Responsable Développement", "InTouch", "intouchgroup.net", "Fintech", "120", "ibrahima.cisse@intouchgroup.net", "+221786789012"},
                {"Aminata", "Touré", "Directrice Commerciale", "BIMA Sénégal", "bima.sn", "Insurtech", "60", "aminata.toure@bima.sn", "+221767890123"}
        };

        int size = Math.min(req.getPerPage() > 0 ? req.getPerPage() : 25, mockPeopleData.length);
        for (int i = 0; i < size; i++) {
            String[] data = mockPeopleData[i];
            String id = "apollo-mock-person-" + (i + 1);

            ApolloOrganization org = ApolloOrganization.builder()
                    .id("apollo-mock-org-" + (i + 1))
                    .name(data[3])
                    .primaryDomain(data[4])
                    .websiteUrl("https://" + data[4])
                    .linkedinUrl("https://linkedin.com/company/" + data[3].toLowerCase().replaceAll("\\s+", "-"))
                    .industry(data[5])
                    .estimatedNumEmployees(Integer.parseInt(data[6]))
                    .country(country)
                    .city(city)
                    .build();

            ApolloPerson person = ApolloPerson.builder()
                    .id(id)
                    .firstName(data[0])
                    .lastName(data[1])
                    .name(data[0] + " " + data[1])
                    .title(data[2])
                    .headline(data[2] + " chez " + data[3])
                    .linkedinUrl("https://linkedin.com/in/" + (data[0] + "-" + data[1]).toLowerCase())
                    .organizationId(org.getId())
                    .organization(org)
                    .country(country)
                    .city(city)
                    .email(null) // Discovery doesn't return emails by default
                    .phoneNumbers(List.of())
                    .build();

            people.add(person);
        }

        ApolloPagination pagination = ApolloPagination.builder()
                .page(req.getPage())
                .perPage(req.getPerPage())
                .totalEntries(100L)
                .totalPages(4)
                .build();

        return ApolloPeopleSearchResponse.builder()
                .people(people)
                .pagination(pagination)
                .totalEntries(100L)
                .build();
    }

    private ApolloOrgSearchResponse generateMockOrgSearchResponse(ApolloOrgSearchRequest req) {
        String loc = (req.getOrganizationLocations() != null && !req.getOrganizationLocations().isEmpty()) ? req.getOrganizationLocations().get(0) : "Senegal";
        String country = loc.contains("Senegal") || loc.contains("SN") ? "Senegal" : "Senegal";
        String city = "Dakar";

        String[][] mockOrgData = {
                {"Gainde 2000", "gainde2000.sn", "Technology & Trade Facilitation", "150", "+221338890000"},
                {"Wave Mobile Money", "wave.com", "Fintech & Mobile Money", "350", "+221338000000"},
                {"Sonatel Orange", "orange.sn", "Telecommunications", "2000", "+221338391200"},
                {"Atos Sénégal", "atos.net", "IT Consulting & Digital Transformation", "200", "+221338491000"},
                {"InTouch Group", "intouchgroup.net", "Fintech & Payment Aggregator", "120", "+221338692000"},
                {"Touchpoint SN", "touchpoint.sn", "Software Engineering & Cloud", "45", "+221338245000"}
        };

        List<ApolloOrganization> orgs = new ArrayList<>();
        int size = Math.min(req.getPerPage() > 0 ? req.getPerPage() : 25, mockOrgData.length);
        for (int i = 0; i < size; i++) {
            String[] data = mockOrgData[i];
            orgs.add(ApolloOrganization.builder()
                    .id("apollo-mock-org-" + (i + 1))
                    .name(data[0])
                    .primaryDomain(data[1])
                    .websiteUrl("https://" + data[1])
                    .linkedinUrl("https://linkedin.com/company/" + data[0].toLowerCase().replaceAll("\\s+", "-"))
                    .industry(data[2])
                    .estimatedNumEmployees(Integer.parseInt(data[3]))
                    .phone(data[4])
                    .country(country)
                    .city(city)
                    .build());
        }

        ApolloPagination pagination = ApolloPagination.builder()
                .page(req.getPage())
                .perPage(req.getPerPage())
                .totalEntries(50L)
                .totalPages(2)
                .build();

        return ApolloOrgSearchResponse.builder()
                .organizations(orgs)
                .pagination(pagination)
                .totalEntries(50L)
                .build();
    }

    private ApolloPersonMatchResponse generateMockPersonMatchResponse(ApolloPersonMatchRequest req) {
        String email = req.getFirstName() != null && req.getLastName() != null
                ? req.getFirstName().toLowerCase() + "." + req.getLastName().toLowerCase() + "@" + (req.getDomain() != null ? req.getDomain() : "prospecta-enriched.sn")
                : "contact.prospect@prospecta-enriched.sn";

        ApolloPerson person = ApolloPerson.builder()
                .id(req.getId() != null ? req.getId() : "apollo-enriched-" + UUID.randomUUID())
                .firstName(req.getFirstName() != null ? req.getFirstName() : "Mamadou")
                .lastName(req.getLastName() != null ? req.getLastName() : "Diop")
                .name(req.getName())
                .email(email)
                .phoneNumbers(List.of(
                        new ApolloPerson.ApolloPhoneNumber("+221776543210", "+221776543210", "mobile")
                ))
                .linkedinUrl("https://linkedin.com/in/" + (req.getFirstName() != null ? req.getFirstName() : "prospect").toLowerCase())
                .country("Senegal")
                .city("Dakar")
                .organization(ApolloOrganization.builder()
                        .name(req.getOrganizationName() != null ? req.getOrganizationName() : "Entreprise Partenaire")
                        .primaryDomain(req.getDomain() != null ? req.getDomain() : "entreprise.sn")
                        .build())
                .build();

        return ApolloPersonMatchResponse.builder()
                .person(person)
                .matches(List.of(person))
                .build();
    }
}
