package com.prospecta.messaging.service;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.CommunicationConsent;
import com.prospecta.messaging.domain.ConsentStatus;
import com.prospecta.messaging.repository.CommunicationConsentRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.domain.ProspectStatus;
import com.prospecta.prospect.repository.ProspectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final CommunicationConsentRepository consentRepository;
    private final ProspectRepository prospectRepository;

    @Transactional(readOnly = true)
    public boolean canContact(UUID organizationId, UUID prospectId, ChannelType channel) {
        Optional<Prospect> prospectOpt = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId);
        if (prospectOpt.isPresent() && prospectOpt.get().getStatus() == ProspectStatus.OPTED_OUT) {
            log.info("Contact blocked: Prospect [{}] has globally OPTED_OUT", prospectId);
            return false;
        }

        Optional<CommunicationConsent> consentOpt = consentRepository
                .findByOrganizationIdAndProspectIdAndChannel(organizationId, prospectId, channel);

        if (consentOpt.isPresent() && consentOpt.get().getStatus() == ConsentStatus.OPTED_OUT) {
            log.info("Contact blocked: Prospect [{}] opted out of channel [{}]", prospectId, channel);
            return false;
        }

        return true;
    }

    @Transactional
    public void recordConsent(UUID organizationId, UUID prospectId, ChannelType channel, ConsentStatus status, String source) {
        Optional<CommunicationConsent> existing = consentRepository
                .findByOrganizationIdAndProspectIdAndChannel(organizationId, prospectId, channel);

        Prospect prospect = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId)
                .orElse(null);

        if (prospect == null) return;

        CommunicationConsent consent = existing.orElseGet(() -> {
            CommunicationConsent c = CommunicationConsent.builder()
                    .prospect(prospect)
                    .channel(channel)
                    .build();
            c.setOrganizationId(organizationId);
            return c;
        });

        consent.setStatus(status);
        consent.setSource(source);
        if (status == ConsentStatus.OPTED_IN) {
            consent.setObtainedAt(Instant.now());
        } else if (status == ConsentStatus.OPTED_OUT) {
            consent.setRevokedAt(Instant.now());
        }

        consentRepository.save(consent);
        log.info("Recorded consent for prospect [{}] on channel [{}]: {}", prospectId, channel, status);
    }
}
