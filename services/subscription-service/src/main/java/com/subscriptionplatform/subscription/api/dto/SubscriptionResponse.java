package com.subscriptionplatform.subscription.api.dto;

import com.subscriptionplatform.subscription.domain.Plan;
import com.subscriptionplatform.subscription.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        Plan plan,
        SubscriptionStatus status,
        Instant trialEndsAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant canceledAt,
        Instant createdAt
) {}
