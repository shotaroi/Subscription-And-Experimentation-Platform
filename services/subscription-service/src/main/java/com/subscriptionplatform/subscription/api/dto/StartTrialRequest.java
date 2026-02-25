package com.subscriptionplatform.subscription.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartTrialRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @Min(1)
        @Max(90)
        int trialDays
) {}
