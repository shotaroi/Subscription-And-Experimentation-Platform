package com.subscriptionplatform.subscription.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces valid subscription state transitions.
 * Models Spotify-like lifecycle: free → trial → active → past_due → grace_period → canceled/expired
 */
public final class SubscriptionStateMachine {

    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> ALLOWED_TRANSITIONS = Map.of(
            SubscriptionStatus.FREE, EnumSet.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE),
            SubscriptionStatus.TRIALING, EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.ACTIVE, EnumSet.of(SubscriptionStatus.PAST_DUE, SubscriptionStatus.CANCELED, SubscriptionStatus.EXPIRED, SubscriptionStatus.GRACE_PERIOD),
            SubscriptionStatus.PAST_DUE, EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.CANCELED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.GRACE_PERIOD, EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.CANCELED, EnumSet.of(SubscriptionStatus.ACTIVE), // reactivation
            SubscriptionStatus.EXPIRED, EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING) // re-subscribe
    );

    private SubscriptionStateMachine() {
    }

    /**
     * Validates that transitioning from {@code from} to {@code to} is allowed.
     *
     * @throws IllegalStateException if transition is not allowed
     */
    public static void validateTransition(SubscriptionStatus from, SubscriptionStatus to) {
        if (from == to) {
            return;
        }
        Set<SubscriptionStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new IllegalStateException(
                    "Invalid subscription transition: " + from + " -> " + to);
        }
    }

    public static boolean canTransition(SubscriptionStatus from, SubscriptionStatus to) {
        if (from == to) {
            return true;
        }
        Set<SubscriptionStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}
