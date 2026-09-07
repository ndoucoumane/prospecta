package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private ProspectRepository prospectRepository;

    @InjectMocks
    private DeduplicationService deduplicationService;

    @Test
    @DisplayName("Should detect duplicate when email matches in same organization")
    void shouldDetectDuplicateByEmail() {
        UUID orgId = UUID.randomUUID();
        String email = "ousmane.ndiaye@hotel-teranga.sn";

        Prospect existing = Prospect.builder().email(email).build();
        existing.setOrganizationId(orgId);

        when(prospectRepository.findByOrganizationIdAndEmailIgnoreCase(orgId, email))
                .thenReturn(Optional.of(existing));

        Optional<Prospect> duplicate = deduplicationService.findDuplicate(orgId, email, null, null, null, null, null);

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Should detect duplicate when phone or whatsapp matches in same organization")
    void shouldDetectDuplicateByPhone() {
        UUID orgId = UUID.randomUUID();
        String phone = "+221771234567";

        Prospect existing = Prospect.builder().phone(phone).build();
        existing.setOrganizationId(orgId);

        when(prospectRepository.findByOrganizationIdAndPhone(orgId, phone))
                .thenReturn(Optional.of(existing));

        Optional<Prospect> duplicate = deduplicationService.findDuplicate(orgId, null, phone, null, null, null, null);

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("Should detect duplicate by first name + last name + company")
    void shouldDetectDuplicateByNameAndCompany() {
        UUID orgId = UUID.randomUUID();
        Prospect existing = Prospect.builder()
                .firstName("Ousmane")
                .lastName("Ndiaye")
                .companyName("Teranga Hotel")
                .build();
        existing.setOrganizationId(orgId);

        when(prospectRepository.findByOrganizationIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndCompanyNameIgnoreCase(
                orgId, "Ousmane", "Ndiaye", "Teranga Hotel"
        )).thenReturn(Optional.of(existing));

        Optional<Prospect> duplicate = deduplicationService.findDuplicate(
                orgId, null, null, null, "Ousmane", "Ndiaye", "Teranga Hotel"
        );

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getFirstName()).isEqualTo("Ousmane");
    }

    @Test
    @DisplayName("Cross-tenant: Duplicate in Org A should not block creation in Org B")
    void shouldAllowSameEmailInDifferentOrganizations() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        String email = "shared.contact@business.sn";

        when(prospectRepository.findByOrganizationIdAndEmailIgnoreCase(orgB, email))
                .thenReturn(Optional.empty());

        Optional<Prospect> duplicateInOrgB = deduplicationService.findDuplicate(orgB, email, null, null, null, null, null);

        assertThat(duplicateInOrgB).isEmpty();
    }
}
