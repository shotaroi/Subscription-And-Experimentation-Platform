package com.subscriptionplatform.subscription.domain;

/**
 * Subscription lifecycle statuses.
 * Allowed transitions are enforced in SubscriptionStateMachine.
 */
public enum SubscriptionStatus {
    FREE,
    TRIALING,
    ACTIVE,
    PAST_DUE,
    GRACE_PERIOD,
    CANCELED,
    EXPIRED
}
