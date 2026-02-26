package com.subscriptionplatform.subscription.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscriptions_user_id", columnList = "user_id"),
        @Index(name = "idx_subscriptions_status", columnList = "status")
})
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- State transitions (domain logic) ---

    public void startTrial(int trialDays) {
        if (status == SubscriptionStatus.TRIALING) {
            throw new IllegalStateException("User is already in trial");
        }
        SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.TRIALING);
        this.status = SubscriptionStatus.TRIALING;
        this.plan = plan == Plan.FREE ? Plan.INDIVIDUAL : plan;
        this.trialEndsAt = Instant.now().plusSeconds(trialDays * 86400L);
        this.currentPeriodStart = Instant.now();
        this.currentPeriodEnd = trialEndsAt;
    }

    public void activate() {
        SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.ACTIVE);
        this.status = SubscriptionStatus.ACTIVE;
        this.trialEndsAt = null; // no longer in trial
        // Period dates should be set by billing when payment succeeds
    }

    public void markPastDue() {
        SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.PAST_DUE);
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void startGracePeriod() {
        SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.GRACE_PERIOD);
        this.status = SubscriptionStatus.GRACE_PERIOD;
    }

    public void cancel(boolean atPeriodEnd) {
        if (atPeriodEnd) {
            this.cancelAtPeriodEnd = true;
            this.canceledAt = Instant.now();
            // Status stays as-is until period ends
        } else {
            SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.CANCELED);
            this.status = SubscriptionStatus.CANCELED;
            this.canceledAt = Instant.now();
            this.cancelAtPeriodEnd = false;
        }
    }

    public void reactivate() {
        SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.ACTIVE);
        this.status = SubscriptionStatus.ACTIVE;
        this.cancelAtPeriodEnd = false;
        this.canceledAt = null;
    }

    public void expire() {
        SubscriptionStateMachine.validateTransition(status, SubscriptionStatus.EXPIRED);
        this.status = SubscriptionStatus.EXPIRED;
        this.canceledAt = Instant.now();
    }

    public void setCurrentPeriod(Instant start, Instant end) {
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
    }

    // --- Getters / Setters ---

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    /** For initial entity creation only; use domain methods for transitions. */
    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
