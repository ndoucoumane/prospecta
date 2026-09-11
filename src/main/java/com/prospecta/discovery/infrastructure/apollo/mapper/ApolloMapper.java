package com.prospecta.discovery.infrastructure.apollo.mapper;

import com.prospecta.discovery.domain.*;
import com.prospecta.discovery.infrastructure.apollo.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApolloMapper {

    public ApolloPeopleSearchRequest toApolloRequest(PeopleSearchRequest request) {
        List<String> locations = new ArrayList<>();
        if (request.city() != null && !request.city().isBlank() && request.country() != null && !request.country().isBlank()) {
            locations.add(request.city().trim() + ", " + request.country().trim());
        } else if (request.city() != null && !request.city().isBlank()) {
            locations.add(request.city().trim());
        } else if (request.country() != null && !request.country().isBlank()) {
            locations.add(request.country().trim());
        }

        List<String> domains = (request.companyDomain() != null && !request.companyDomain().isBlank())
                ? List.of(request.companyDomain().trim())
                : null;

        List<String> employeeRanges = mapEmployeeRanges(request.companySizeMin(), request.companySizeMax());

        return ApolloPeopleSearchRequest.builder()
                .personTitles(request.jobTitles() != null && !request.jobTitles().isEmpty() ? request.jobTitles() : null)
                .personLocations(locations.isEmpty() ? null : locations)
                .qOrganizationName(request.companyName() != null && !request.companyName().isBlank() ? request.companyName().trim() : null)
                .qOrganizationDomains(domains)
                .organizationNumEmployeesRanges(employeeRanges.isEmpty() ? null : employeeRanges)
                .page(request.page() + 1) // Apollo pagination is 1-indexed
                .perPage(request.size())
                .build();
    }

    public ApolloOrgSearchRequest toApolloRequest(CompanySearchRequest request) {
        List<String> locations = new ArrayList<>();
        if (request.city() != null && !request.city().isBlank() && request.country() != null && !request.country().isBlank()) {
            locations.add(request.city().trim() + ", " + request.country().trim());
        } else if (request.city() != null && !request.city().isBlank()) {
            locations.add(request.city().trim());
        } else if (request.country() != null && !request.country().isBlank()) {
            locations.add(request.country().trim());
        }

        List<String> domains = (request.domain() != null && !request.domain().isBlank())
                ? List.of(request.domain().trim())
                : null;

        List<String> employeeRanges = mapEmployeeRanges(request.companySizeMin(), request.companySizeMax());

        return ApolloOrgSearchRequest.builder()
                .qOrganizationName(request.name() != null && !request.name().isBlank() ? request.name().trim() : null)
                .qOrganizationDomains(domains)
                .organizationLocations(locations.isEmpty() ? null : locations)
                .organizationNumEmployeesRanges(employeeRanges.isEmpty() ? null : employeeRanges)
                .page(request.page() + 1)
                .perPage(request.size())
                .build();
    }

    public DiscoveredPerson toDiscoveredPerson(ApolloPerson apolloPerson) {
        if (apolloPerson == null) {
            return null;
        }

        String companyName = null;
        String companyDomain = null;
        if (apolloPerson.getOrganization() != null) {
            companyName = apolloPerson.getOrganization().getName();
            companyDomain = apolloPerson.getOrganization().getPrimaryDomain() != null
                    ? apolloPerson.getOrganization().getPrimaryDomain()
                    : apolloPerson.getOrganization().getWebsiteUrl();
        }

        String phone = null;
        if (apolloPerson.getPhoneNumbers() != null && !apolloPerson.getPhoneNumbers().isEmpty()) {
            for (ApolloPerson.ApolloPhoneNumber p : apolloPerson.getPhoneNumbers()) {
                if (p.getSanitizedNumber() != null && !p.getSanitizedNumber().isBlank()) {
                    phone = p.getSanitizedNumber();
                    break;
                } else if (p.getRawNumber() != null && !p.getRawNumber().isBlank()) {
                    phone = p.getRawNumber();
                    break;
                }
            }
        }

        return DiscoveredPerson.builder()
                .externalId(apolloPerson.getId())
                .firstName(apolloPerson.getFirstName())
                .lastName(apolloPerson.getLastName())
                .jobTitle(apolloPerson.getTitle() != null ? apolloPerson.getTitle() : apolloPerson.getHeadline())
                .companyName(companyName)
                .companyDomain(companyDomain)
                .linkedinUrl(apolloPerson.getLinkedinUrl())
                .country(apolloPerson.getCountry())
                .city(apolloPerson.getCity())
                .email(apolloPerson.getEmail())
                .phoneNumber(phone)
                .source("APOLLO")
                .build();
    }

    public DiscoveredCompany toDiscoveredCompany(ApolloOrganization org) {
        if (org == null) {
            return null;
        }

        String domain = org.getPrimaryDomain() != null && !org.getPrimaryDomain().isBlank()
                ? org.getPrimaryDomain()
                : org.getWebsiteUrl();

        return DiscoveredCompany.builder()
                .externalId(org.getId())
                .name(org.getName())
                .domain(domain)
                .industry(org.getIndustry())
                .country(org.getCountry())
                .city(org.getCity())
                .employeeCount(org.getEstimatedNumEmployees())
                .linkedinUrl(org.getLinkedinUrl())
                .source("APOLLO")
                .build();
    }

    public PeopleSearchResult toPeopleSearchResult(ApolloPeopleSearchResponse response, int requestedPage, int requestedSize) {
        if (response == null || response.getPeople() == null || response.getPeople().isEmpty()) {
            return PeopleSearchResult.empty(requestedPage, requestedSize, "APOLLO");
        }

        List<DiscoveredPerson> items = response.getPeople().stream()
                .map(this::toDiscoveredPerson)
                .toList();

        return PeopleSearchResult.builder()
                .items(items)
                .page(requestedPage)
                .size(requestedSize)
                .total(response.resolveTotalEntries())
                .source("APOLLO")
                .build();
    }

    public CompanySearchResult toCompanySearchResult(ApolloOrgSearchResponse response, int requestedPage, int requestedSize) {
        if (response == null || response.getOrganizations() == null || response.getOrganizations().isEmpty()) {
            return CompanySearchResult.empty(requestedPage, requestedSize, "APOLLO");
        }

        List<DiscoveredCompany> items = response.getOrganizations().stream()
                .map(this::toDiscoveredCompany)
                .toList();

        return CompanySearchResult.builder()
                .items(items)
                .page(requestedPage)
                .size(requestedSize)
                .total(response.resolveTotalEntries())
                .source("APOLLO")
                .build();
    }

    private List<String> mapEmployeeRanges(Integer min, Integer max) {
        List<String> ranges = new ArrayList<>();
        if (min == null && max == null) {
            return ranges;
        }
        int low = (min != null) ? min : 1;
        int high = (max != null) ? max : Integer.MAX_VALUE;

        // Standard Apollo size brackets
        if (overlaps(low, high, 1, 10)) ranges.add("1,10");
        if (overlaps(low, high, 11, 20)) ranges.add("11,20");
        if (overlaps(low, high, 21, 50)) ranges.add("21,50");
        if (overlaps(low, high, 51, 100)) ranges.add("51,100");
        if (overlaps(low, high, 101, 200)) ranges.add("101,200");
        if (overlaps(low, high, 201, 500)) ranges.add("201,500");
        if (overlaps(low, high, 501, 1000)) ranges.add("501,1000");
        if (overlaps(low, high, 1001, 5000)) ranges.add("1001,5000");
        if (overlaps(low, high, 5001, 10000)) ranges.add("5001,10000");
        if (overlaps(low, high, 10001, Integer.MAX_VALUE)) ranges.add("10001+");

        return ranges;
    }

    private boolean overlaps(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) <= Math.min(end1, end2);
    }
}
