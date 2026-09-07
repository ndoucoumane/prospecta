package com.prospecta.messaging.provider;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.messaging.domain.Message;

public interface MessagingProvider {

    MessageSendResult send(Message message);

    boolean supports(ChannelType channel);
}
