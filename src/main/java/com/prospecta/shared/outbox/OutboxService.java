package com.prospecta.shared.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status("PENDING")
                    .build();

            outboxRepository.save(event);
            log.debug("Outbox event recorded: [type={}, aggregateId={}]", eventType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event payload for type: {}", eventType, e);
            throw new RuntimeException("Could not serialize outbox event payload", e);
        }
    }

    @Scheduled(fixedDelayString = "${prospecta.outbox.poll-interval-ms:5000}")
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTopPendingEvents("PENDING", PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());
        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
    }

    @Transactional
    public void publishEvent(OutboxEvent event) {
        try {
            String topic = resolveTopicName(event.getEventType());
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            event.setStatus("PUBLISHED");
                            event.setPublishedAt(Instant.now());
                            outboxRepository.save(event);
                            log.debug("Published outbox event [{}] to topic [{}]", event.getId(), topic);
                        } else {
                            log.warn("Failed to publish outbox event [{}] to Kafka: {}", event.getId(), ex.getMessage());
                            event.setRetryCount(event.getRetryCount() + 1);
                            if (event.getRetryCount() > 5) {
                                event.setStatus("FAILED");
                            }
                            outboxRepository.save(event);
                        }
                    });
        } catch (Exception e) {
            log.warn("Error initiating Kafka publish for outbox event [{}]: {}", event.getId(), e.getMessage());
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() > 5) {
                event.setStatus("FAILED");
            }
            outboxRepository.save(event);
        }
    }

    private String resolveTopicName(String eventType) {
        // e.g. prospect.created -> prospecta.prospect.events.v1
        if (eventType.startsWith("prospect.")) {
            return "prospecta.prospect.events.v1";
        } else if (eventType.startsWith("campaign.")) {
            return "prospecta.campaign.events.v1";
        } else if (eventType.startsWith("message.")) {
            return "prospecta.message.events.v1";
        } else if (eventType.startsWith("whatsapp.")) {
            return "prospecta.whatsapp.events.v1";
        }
        return "prospecta.general.events.v1";
    }
}
