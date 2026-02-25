package com.subscriptionplatform.subscription.application;

import com.subscriptionplatform.subscription.api.ResourceNotFoundException;
import com.subscriptionplatform.subscription.api.dto.SubscriptionResponse;
import com.subscriptionplatform.subscription.domain.Plan;
import com.subscriptionplatform.subscription.domain.Subscription;
import com.subscriptionplatform.subscription.domain.SubscriptionStatus;
import com.subscriptionplatform.subscription.repository.SubscriptionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubscriptionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionApplicationService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final OutboxPublisher outboxPublisher;
    private final MeterRegistry meterRegistry;

    public SubscriptionApplicationService(SubscriptionRepository subscriptionRepository,
                                         OutboxPublisher outboxPublisher,
                                         MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.outboxPublisher = outboxPublisher;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionForUser(UUID userId) {
        Subscription sub = subscriptionRepository.findLatestByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for user " + userId));
        return toResponse(sub);
    }

    @Transactional
    public SubscriptionResponse startTrial(UUID userId, int trialDays) {
        Subscription sub = subscriptionRepository.findActiveByUserId(userId)
                .orElseGet(() -> createFreeSubscription(userId));

        SubscriptionStatus from = sub.getStatus();
        sub.startTrial(trialDays);
        Subscription saved = subscriptionRepository.save(sub);

        recordTransition(from, sub.getStatus(), saved.getId(), userId);
        outboxPublisher.publishTrialStarted(saved);

        log.info("Trial started for userId={} subscriptionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID userId, boolean atPeriodEnd) {
        Subscription sub = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user " + userId));

        sub.cancel(atPeriodEnd);
        Subscription saved = subscriptionRepository.save(sub);

        outboxPublisher.publishCanceled(saved);

        log.info("Subscription canceled for userId={} subscriptionId={} atPeriodEnd={}", userId, saved.getId(), atPeriodEnd);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse reactivate(UUID userId) {
        Subscription sub = subscriptionRepository.findLatestByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for user " + userId));

        SubscriptionStatus from = sub.getStatus();
        sub.reactivate();
        Subscription saved = subscriptionRepository.save(sub);

        recordTransition(from, sub.getStatus(), saved.getId(), userId);
        outboxPublisher.publishReactivated(saved);

        log.info("Subscription reactivated for userId={} subscriptionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    private Subscription createFreeSubscription(UUID userId) {
        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setPlan(Plan.FREE);
        sub.setStatus(SubscriptionStatus.FREE);
        return subscriptionRepository.save(sub);
    }

    private void recordTransition(SubscriptionStatus from, SubscriptionStatus to, UUID subscriptionId, UUID userId) {
        meterRegistry.counter("subscriptions_state_transition_total",
                "from", from.name(),
                "to", to.name()).increment();
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getUserId(),
                s.getPlan(),
                s.getStatus(),
                s.getTrialEndsAt(),
                s.getCurrentPeriodStart(),
                s.getCurrentPeriodEnd(),
                s.isCancelAtPeriodEnd(),
                s.getCanceledAt(),
                s.getCreatedAt()
        );
    }
}
