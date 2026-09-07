package com.prospecta.messaging.provider;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class EmailMessagingProvider implements MessagingProvider {

    @Value("${prospecta.mail.host:localhost}")
    private String mailHost;

    @Value("${prospecta.mail.port:1025}")
    private int mailPort;

    @Override
    public boolean supports(ChannelType channel) {
        return channel == ChannelType.EMAIL;
    }

    @Override
    public MessageSendResult send(Message message) {
        log.info("Dispatching email to [{}] (subject='{}')", message.getRecipient(), message.getSubject());
        // For development, Mailpit acts as local SMTP server
        String externalId = "msg_email_" + UUID.randomUUID().toString().replace("-", "");
        return MessageSendResult.success(externalId);
    }
}
