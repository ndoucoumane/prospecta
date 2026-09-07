package com.prospecta.messaging.service;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.Message;
import com.prospecta.messaging.domain.MessageDirection;
import com.prospecta.messaging.domain.MessageStatus;
import com.prospecta.messaging.provider.MessageSendResult;
import com.prospecta.messaging.provider.MessagingProvider;
import com.prospecta.messaging.repository.MessageRepository;
import com.prospecta.prospect.domain.Prospect;
import com.prospecta.prospect.repository.ProspectRepository;
import com.prospecta.shared.audit.AuditService;
import com.prospecta.shared.exception.ProspectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final MessageRepository messageRepository;
    private final ProspectRepository prospectRepository;
    private final List<MessagingProvider> providers;
    private final ConsentService consentService;
    private final AuditService auditService;

    @Transactional
    public Message sendMessage(
            UUID organizationId,
            UUID prospectId,
            UUID campaignId,
            ChannelType channel,
            String recipient,
            String subject,
            String content
    ) {
        // 1. Consent check
        if (!consentService.canContact(organizationId, prospectId, channel)) {
            log.warn("Cannot send message: Prospect [{}] has opted out of [{}]", prospectId, channel);
            throw new IllegalStateException("Prospect has opted out of communication on channel: " + channel);
        }

        Prospect prospect = prospectRepository.findByIdAndOrganizationId(prospectId, organizationId)
                .orElseThrow(() -> new ProspectNotFoundException(prospectId));

        // 2. Select appropriate provider
        MessagingProvider provider = providers.stream()
                .filter(p -> p.supports(channel))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("No messaging provider registered for channel: " + channel));

        Message message = Message.builder()
                .campaignId(campaignId)
                .prospect(prospect)
                .channel(channel)
                .direction(MessageDirection.OUTBOUND)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .status(MessageStatus.SENDING)
                .build();
        message.setOrganizationId(organizationId);

        Message saved = messageRepository.save(message);

        // 3. Dispatch to provider
        MessageSendResult result = provider.send(saved);

        if (result.isSuccess()) {
            saved.setStatus(MessageStatus.SENT);
            saved.setExternalId(result.getExternalId());
            saved.setSentAt(Instant.now());
        } else {
            saved.setStatus(MessageStatus.FAILED);
            saved.setErrorMessage(result.getErrorMessage());
        }

        Message updated = messageRepository.save(saved);
        auditService.logSync("MESSAGE_SENT", "Message", updated.getId().toString(),
                String.format("Channel: %s, Status: %s, ExtId: %s", channel, updated.getStatus(), updated.getExternalId()));

        return updated;
    }
}
