package com.subscriptionplatform.subscription.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void startTrial_fromFree_succeeds() {
        Subscription sub = createFreeSubscription();
        sub.startTrial(14);
        assertEquals(SubscriptionStatus.TRIALING, sub.getStatus());
        assertEquals(Plan.INDIVIDUAL, sub.getPlan());
        assertNotNull(sub.getTrialEndsAt());
    }

    @Test
    void startTrial_fromTrialing_throws() {
        Subscription sub = createFreeSubscription();
        sub.startTrial(14);
        assertThrows(IllegalStateException.class, () -> sub.startTrial(7));
    }

    @Test
    void cancel_atPeriodEnd_setsFlag() {
        Subscription sub = createActiveSubscription();
        sub.cancel(true);
        assertTrue(sub.isCancelAtPeriodEnd());
        assertNotNull(sub.getCanceledAt());
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus()); // stays active until period end
    }

    @Test
    void cancel_immediate_transitionsToCanceled() {
        Subscription sub = createActiveSubscription();
        sub.cancel(false);
        assertEquals(SubscriptionStatus.CANCELED, sub.getStatus());
        assertNotNull(sub.getCanceledAt());
    }

    @Test
    void reactivate_fromCanceled_succeeds() {
        Subscription sub = createActiveSubscription();
        sub.cancel(false);
        sub.reactivate();
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertFalse(sub.isCancelAtPeriodEnd());
        assertNull(sub.getCanceledAt());
    }

    @Test
    void markPastDue_fromActive_succeeds() {
        Subscription sub = createActiveSubscription();
        sub.markPastDue();
        assertEquals(SubscriptionStatus.PAST_DUE, sub.getStatus());
    }

    @Test
    void startGracePeriod_fromPastDue_succeeds() {
        Subscription sub = createActiveSubscription();
        sub.markPastDue();
        sub.startGracePeriod();
        assertEquals(SubscriptionStatus.GRACE_PERIOD, sub.getStatus());
    }

    private Subscription createFreeSubscription() {
        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setPlan(Plan.FREE);
        sub.setStatus(SubscriptionStatus.FREE);
        return sub;
    }

    private Subscription createActiveSubscription() {
        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setPlan(Plan.INDIVIDUAL);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        return sub;
    }
}
