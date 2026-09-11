package com.prospecta.discovery.infrastructure.apollo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.discovery.domain.DiscoveryErrorCode;
import com.prospecta.discovery.domain.DiscoveryException;
import com.prospecta.discovery.infrastructure.apollo.config.ApolloProperties;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPeopleSearchRequest;
import com.prospecta.discovery.infrastructure.apollo.dto.ApolloPeopleSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestClientApolloClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should execute people search with x-api-key header and deserialize Apollo response")
    void shouldExecutePeopleSearchSuccessfully() {
        ApolloProperties props = new ApolloProperties();
        props.setBaseUrl("https://api.apollo.io");
        props.setApiKey("test-real-api-key-12345");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientApolloClient client = new RestClientApolloClient(props, objectMapper, builder);

        String responseJson = """
                {
                    "people": [
                        {
                            "id": "person-1",
                            "first_name": "Mamadou",
                            "last_name": "Diop",
                            "title": "CEO",
                            "linkedin_url": "https://linkedin.com/in/mamadou-diop",
                            "organization": {
                                "id": "org-1",
                                "name": "Gainde 2000",
                                "primary_domain": "gainde2000.sn"
                            }
                        }
                    ],
                    "pagination": {
                        "page": 1,
                        "per_page": 25,
                        "total_entries": 1,
                        "total_pages": 1
                    }
                }
                """;

        server.expect(requestTo("https://api.apollo.io/api/v1/mixed_people/api_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-real-api-key-12345"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        ApolloPeopleSearchRequest request = ApolloPeopleSearchRequest.builder()
                .personTitles(List.of("CEO"))
                .page(1)
                .perPage(25)
                .build();

        ApolloPeopleSearchResponse response = client.searchPeople(request);

        assertThat(response).isNotNull();
        assertThat(response.getPeople()).hasSize(1);
        assertThat(response.getPeople().get(0).getFirstName()).isEqualTo("Mamadou");
        assertThat(response.resolveTotalEntries()).isEqualTo(1L);

        server.verify();
    }

    @Test
    @DisplayName("Should translate Apollo 401 Unauthorized to DiscoveryException(AUTHENTICATION_ERROR)")
    void shouldHandleAuthenticationError() {
        ApolloProperties props = new ApolloProperties();
        props.setBaseUrl("https://api.apollo.io");
        props.setApiKey("invalid-key-xyz");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientApolloClient client = new RestClientApolloClient(props, objectMapper, builder);

        server.expect(requestTo("https://api.apollo.io/api/v1/mixed_people/api_search"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid API Key\"}"));

        ApolloPeopleSearchRequest request = ApolloPeopleSearchRequest.builder().page(1).perPage(25).build();

        assertThatThrownBy(() -> client.searchPeople(request))
                .isInstanceOf(DiscoveryException.class)
                .satisfies(ex -> {
                    DiscoveryException de = (DiscoveryException) ex;
                    assertThat(de.getDiscoveryErrorCode()).isEqualTo(DiscoveryErrorCode.AUTHENTICATION_ERROR);
                });

        server.verify();
    }

    @Test
    @DisplayName("Should translate Apollo 429 Too Many Requests to DiscoveryException(RATE_LIMITED)")
    void shouldHandleRateLimitError() {
        ApolloProperties props = new ApolloProperties();
        props.setBaseUrl("https://api.apollo.io");
        props.setApiKey("valid-key-xyz");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientApolloClient client = new RestClientApolloClient(props, objectMapper, builder);

        server.expect(requestTo("https://api.apollo.io/api/v1/mixed_people/api_search"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\": \"Rate limit exceeded\"}"));

        ApolloPeopleSearchRequest request = ApolloPeopleSearchRequest.builder().page(1).perPage(25).build();

        assertThatThrownBy(() -> client.searchPeople(request))
                .isInstanceOf(DiscoveryException.class)
                .satisfies(ex -> {
                    DiscoveryException de = (DiscoveryException) ex;
                    assertThat(de.getDiscoveryErrorCode()).isEqualTo(DiscoveryErrorCode.RATE_LIMITED);
                });

        server.verify();
    }

    @Test
    @DisplayName("Should translate Apollo 503 Service Unavailable to DiscoveryException(PROVIDER_UNAVAILABLE)")
    void shouldHandleProviderUnavailableError() {
        ApolloProperties props = new ApolloProperties();
        props.setBaseUrl("https://api.apollo.io");
        props.setApiKey("valid-key-xyz");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClientApolloClient client = new RestClientApolloClient(props, objectMapper, builder);

        server.expect(requestTo("https://api.apollo.io/api/v1/mixed_people/api_search"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ApolloPeopleSearchRequest request = ApolloPeopleSearchRequest.builder().page(1).perPage(25).build();

        assertThatThrownBy(() -> client.searchPeople(request))
                .isInstanceOf(DiscoveryException.class)
                .satisfies(ex -> {
                    DiscoveryException de = (DiscoveryException) ex;
                    assertThat(de.getDiscoveryErrorCode()).isEqualTo(DiscoveryErrorCode.PROVIDER_UNAVAILABLE);
                });

        server.verify();
    }

    @Test
    @DisplayName("Should return simulated mock data when API key is empty (local dev mode)")
    void shouldReturnMockDataWhenApiKeyEmpty() {
        ApolloProperties props = new ApolloProperties();
        props.setApiKey(""); // empty

        RestClient.Builder builder = RestClient.builder();
        RestClientApolloClient client = new RestClientApolloClient(props, objectMapper, builder);

        ApolloPeopleSearchRequest request = ApolloPeopleSearchRequest.builder()
                .personTitles(List.of("Directeur commercial"))
                .personLocations(List.of("Dakar, SN"))
                .page(1)
                .perPage(5)
                .build();

        ApolloPeopleSearchResponse response = client.searchPeople(request);

        assertThat(response).isNotNull();
        assertThat(response.getPeople()).isNotEmpty();
        assertThat(response.getPeople().get(0).getCountry()).isEqualTo("Senegal");
        assertThat(response.resolveTotalEntries()).isGreaterThan(0);
    }
}
