package com.subscriptionplatform.subscription.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionStateMachineTest {

    @Test
    void freeToTrialing_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.FREE, SubscriptionStatus.TRIALING));
        assertDoesNotThrow(() -> SubscriptionStateMachine.validateTransition(SubscriptionStatus.FREE, SubscriptionStatus.TRIALING));
    }

    @Test
    void freeToActive_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.FREE, SubscriptionStatus.ACTIVE));
    }

    @Test
    void trialingToActive_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE));
    }

    @Test
    void trialingToCanceled_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.TRIALING, SubscriptionStatus.CANCELED));
    }

    @Test
    void activeToPastDue_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE));
    }

    @Test
    void activeToGracePeriod_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD));
    }

    @Test
    void pastDueToGracePeriod_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.PAST_DUE, SubscriptionStatus.GRACE_PERIOD));
    }

    @Test
    void gracePeriodToCanceled_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.CANCELED));
    }

    @Test
    void canceledToActive_isAllowed_reactivation() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.CANCELED, SubscriptionStatus.ACTIVE));
    }

    @Test
    void freeToCanceled_isNotAllowed() {
        assertFalse(SubscriptionStateMachine.canTransition(SubscriptionStatus.FREE, SubscriptionStatus.CANCELED));
        assertThrows(IllegalStateException.class,
                () -> SubscriptionStateMachine.validateTransition(SubscriptionStatus.FREE, SubscriptionStatus.CANCELED));
    }

    @Test
    void activeToTrialing_isNotAllowed() {
        assertFalse(SubscriptionStateMachine.canTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING));
    }

    @Test
    void sameStatus_isAllowed() {
        assertTrue(SubscriptionStateMachine.canTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE));
        assertDoesNotThrow(() -> SubscriptionStateMachine.validateTransition(SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE));
    }
}
