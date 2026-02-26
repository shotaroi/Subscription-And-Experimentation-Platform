package com.subscriptionplatform.subscription.infrastructure;

import com.subscriptionplatform.subscription.domain.OutboxEvent;
import com.subscriptionplatform.subscription.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OutboxKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxKafkaPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxKafkaPublisher(OutboxEventRepository outboxEventRepository,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishOutboxEvents() {
        var unsent = outboxEventRepository.findUnsentEvents();
        for (OutboxEvent event : unsent) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get();
                event.setSentAt(Instant.now());
                outboxEventRepository.save(event);
                log.debug("Published outbox event id={} to topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event id={}: {}", event.getId(), e.getMessage());
                break; // Retry next cycle
            }
        }
    }
}
