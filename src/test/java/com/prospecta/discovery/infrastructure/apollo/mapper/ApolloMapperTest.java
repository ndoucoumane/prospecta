package com.prospecta.discovery.infrastructure.apollo.mapper;

import com.prospecta.discovery.domain.*;
import com.prospecta.discovery.infrastructure.apollo.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApolloMapperTest {

    private ApolloMapper apolloMapper;

    @BeforeEach
    void setUp() {
        apolloMapper = new ApolloMapper();
    }

    @Test
    @DisplayName("Should map PeopleSearchRequest to ApolloPeopleSearchRequest with 1-indexed page and locations")
    void shouldMapPeopleSearchRequestToApollo() {
        PeopleSearchRequest request = PeopleSearchRequest.builder()
                .jobTitles(List.of("CEO", "Directeur commercial"))
                .companyName("Gainde 2000")
                .companyDomain("gainde2000.sn")
                .country("SN")
                .city("Dakar")
                .companySizeMin(10)
                .companySizeMax(200)
                .page(0)
                .size(25)
                .build();

        ApolloPeopleSearchRequest apolloReq = apolloMapper.toApolloRequest(request);

        assertThat(apolloReq.getPage()).isEqualTo(1); // 0-index -> 1-index
        assertThat(apolloReq.getPerPage()).isEqualTo(25);
        assertThat(apolloReq.getPersonTitles()).containsExactly("CEO", "Directeur commercial");
        assertThat(apolloReq.getQOrganizationName()).isEqualTo("Gainde 2000");
        assertThat(apolloReq.getQOrganizationDomains()).containsExactly("gainde2000.sn");
        assertThat(apolloReq.getPersonLocations()).containsExactly("Dakar, SN");
        assertThat(apolloReq.getOrganizationNumEmployeesRanges()).contains("1,10", "11,20", "21,50", "51,100", "101,200");
    }

    @Test
    @DisplayName("Should map ApolloPerson to DiscoveredPerson correctly")
    void shouldMapApolloPersonToDiscoveredPerson() {
        ApolloOrganization org = ApolloOrganization.builder()
                .id("org-123")
                .name("Sonatel")
                .primaryDomain("orange.sn")
                .industry("Telecommunications")
                .estimatedNumEmployees(2000)
                .build();

        ApolloPerson person = ApolloPerson.builder()
                .id("apollo-p-1")
                .firstName("Awa")
                .lastName("Fall")
                .title("Head of Sales")
                .linkedinUrl("https://linkedin.com/in/awa-fall")
                .organization(org)
                .country("Senegal")
                .city("Dakar")
                .email("awa.fall@orange.sn")
                .phoneNumbers(List.of(new ApolloPerson.ApolloPhoneNumber("+221773456789", "+221773456789", "mobile")))
                .build();

        DiscoveredPerson discovered = apolloMapper.toDiscoveredPerson(person);

        assertThat(discovered.externalId()).isEqualTo("apollo-p-1");
        assertThat(discovered.firstName()).isEqualTo("Awa");
        assertThat(discovered.lastName()).isEqualTo("Fall");
        assertThat(discovered.jobTitle()).isEqualTo("Head of Sales");
        assertThat(discovered.companyName()).isEqualTo("Sonatel");
        assertThat(discovered.companyDomain()).isEqualTo("orange.sn");
        assertThat(discovered.phoneNumber()).isEqualTo("+221773456789");
        assertThat(discovered.email()).isEqualTo("awa.fall@orange.sn");
        assertThat(discovered.source()).isEqualTo("APOLLO");
    }

    @Test
    @DisplayName("Should map ApolloPeopleSearchResponse to normalized PeopleSearchResult")
    void shouldMapPeopleSearchResponse() {
        ApolloPerson person = ApolloPerson.builder()
                .id("apollo-p-2")
                .firstName("Mamadou")
                .lastName("Diop")
                .title("CEO")
                .build();

        ApolloPeopleSearchResponse response = ApolloPeopleSearchResponse.builder()
                .people(List.of(person))
                .pagination(ApolloPagination.builder().page(1).perPage(25).totalEntries(42L).totalPages(2).build())
                .build();

        PeopleSearchResult result = apolloMapper.toPeopleSearchResult(response, 0, 25);

        assertThat(result.items()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(25);
        assertThat(result.total()).isEqualTo(42L);
        assertThat(result.source()).isEqualTo("APOLLO");
    }

    @Test
    @DisplayName("Should map CompanySearchRequest and ApolloOrgSearchResponse")
    void shouldMapCompanySearchRequestAndResponse() {
        CompanySearchRequest request = CompanySearchRequest.builder()
                .name("Gainde")
                .country("SN")
                .city("Dakar")
                .page(1)
                .size(20)
                .build();

        ApolloOrgSearchRequest apolloReq = apolloMapper.toApolloRequest(request);
        assertThat(apolloReq.getPage()).isEqualTo(2);
        assertThat(apolloReq.getPerPage()).isEqualTo(20);
        assertThat(apolloReq.getQOrganizationName()).isEqualTo("Gainde");

        ApolloOrganization org = ApolloOrganization.builder()
                .id("org-99")
                .name("Gainde 2000")
                .primaryDomain("gainde2000.sn")
                .industry("Technology")
                .estimatedNumEmployees(150)
                .country("Senegal")
                .city("Dakar")
                .build();

        ApolloOrgSearchResponse response = ApolloOrgSearchResponse.builder()
                .organizations(List.of(org))
                .pagination(ApolloPagination.builder().page(2).perPage(20).totalEntries(35L).build())
                .build();

        CompanySearchResult result = apolloMapper.toCompanySearchResult(response, 1, 20);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Gainde 2000");
        assertThat(result.items().get(0).domain()).isEqualTo("gainde2000.sn");
        assertThat(result.total()).isEqualTo(35L);
    }
}
