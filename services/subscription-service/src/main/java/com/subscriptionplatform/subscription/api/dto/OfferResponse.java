package com.subscriptionplatform.subscription.api.dto;

import com.subscriptionplatform.subscription.domain.Plan;

import java.util.UUID;

public record OfferResponse(
        UUID userId,
        String experimentKey,
        String variant,
        Plan plan,
        String offerDescription,
        int discountPercent
) {}
