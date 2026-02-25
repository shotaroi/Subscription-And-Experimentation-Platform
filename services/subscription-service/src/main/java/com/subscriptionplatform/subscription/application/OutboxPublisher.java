package com.subscriptionplatform.subscription.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionplatform.subscription.domain.OutboxEvent;
import com.subscriptionplatform.subscription.domain.Subscription;
import com.subscriptionplatform.subscription.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private static final String TOPIC_SUBSCRIPTION_EVENTS = "subscription.events";
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void publishTrialStarted(Subscription s) {
        publish(TOPIC_SUBSCRIPTION_EVENTS, "trial_started", "subscription", s.getId().toString(),
                Map.of(
                        "subscriptionId", s.getId().toString(),
                        "userId", s.getUserId().toString(),
                        "plan", s.getPlan().name(),
                        "status", s.getStatus().name(),
                        "trialEndsAt", s.getTrialEndsAt() != null ? s.getTrialEndsAt().toString() : null
                ));
    }

    public void publishActivated(Subscription s) {
        publish(TOPIC_SUBSCRIPTION_EVENTS, "activated", "subscription", s.getId().toString(),
                Map.of(
                        "subscriptionId", s.getId().toString(),
                        "userId", s.getUserId().toString(),
                        "plan", s.getPlan().name(),
                        "status", s.getStatus().name()
                ));
    }

    public void publishCanceled(Subscription s) {
        publish(TOPIC_SUBSCRIPTION_EVENTS, "canceled", "subscription", s.getId().toString(),
                Map.of(
                        "subscriptionId", s.getId().toString(),
                        "userId", s.getUserId().toString(),
                        "cancelAtPeriodEnd", s.isCancelAtPeriodEnd(),
                        "canceledAt", s.getCanceledAt() != null ? s.getCanceledAt().toString() : null
                ));
    }

    public void publishPastDue(Subscription s) {
        publish(TOPIC_SUBSCRIPTION_EVENTS, "past_due", "subscription", s.getId().toString(),
                Map.of(
                        "subscriptionId", s.getId().toString(),
                        "userId", s.getUserId().toString(),
                        "plan", s.getPlan().name()
                ));
    }

    public void publishGraceStarted(Subscription s) {
        publish(TOPIC_SUBSCRIPTION_EVENTS, "grace_started", "subscription", s.getId().toString(),
                Map.of(
                        "subscriptionId", s.getId().toString(),
                        "userId", s.getUserId().toString(),
                        "plan", s.getPlan().name()
                ));
    }

    public void publishReactivated(Subscription s) {
        publish(TOPIC_SUBSCRIPTION_EVENTS, "reactivated", "subscription", s.getId().toString(),
                Map.of(
                        "subscriptionId", s.getId().toString(),
                        "userId", s.getUserId().toString(),
                        "plan", s.getPlan().name()
                ));
    }

    private void publish(String topic, String eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent();
            event.setTopic(topic);
            event.setPayload(json);
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
