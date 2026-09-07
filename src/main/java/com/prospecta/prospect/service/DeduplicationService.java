package com.prospecta.prospect.service;

import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final ProspectRepository prospectRepository;

    @Transactional(readOnly = true)
    public Optional<Prospect> findDuplicate(
            UUID organizationId,
            String email,
            String phone,
            String whatsappNumber,
            String firstName,
            String lastName,
            String companyName
    ) {
        // 1. Deduplication by email
        if (email != null && !email.isBlank()) {
            Optional<Prospect> byEmail = prospectRepository.findByOrganizationIdAndEmailIgnoreCase(organizationId, email.trim());
            if (byEmail.isPresent()) {
                log.debug("Duplicate prospect detected by email [{}] for organization [{}]", email, organizationId);
                return byEmail;
            }
        }

        // 2. Deduplication by WhatsApp number
        if (whatsappNumber != null && !whatsappNumber.isBlank()) {
            Optional<Prospect> byWhatsapp = prospectRepository.findByOrganizationIdAndWhatsappNumber(organizationId, whatsappNumber.trim());
            if (byWhatsapp.isPresent()) {
                log.debug("Duplicate prospect detected by whatsapp [{}] for organization [{}]", whatsappNumber, organizationId);
                return byWhatsapp;
            }
        }

        // 3. Deduplication by phone number
        if (phone != null && !phone.isBlank()) {
            Optional<Prospect> byPhone = prospectRepository.findByOrganizationIdAndPhone(organizationId, phone.trim());
            if (byPhone.isPresent()) {
                log.debug("Duplicate prospect detected by phone [{}] for organization [{}]", phone, organizationId);
                return byPhone;
            }
        }

        // 4. Deduplication by combination: firstName + lastName + companyName
        if (firstName != null && !firstName.isBlank() &&
                lastName != null && !lastName.isBlank() &&
                companyName != null && !companyName.isBlank()) {
            Optional<Prospect> byNameAndCompany = prospectRepository
                    .findByOrganizationIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndCompanyNameIgnoreCase(
                            organizationId, firstName.trim(), lastName.trim(), companyName.trim()
                    );
            if (byNameAndCompany.isPresent()) {
                log.debug("Duplicate prospect detected by name [{}] and company [{}] for organization [{}]",
                        firstName + " " + lastName, companyName, organizationId);
                return byNameAndCompany;
            }
        }

        return Optional.empty();
    }
}
