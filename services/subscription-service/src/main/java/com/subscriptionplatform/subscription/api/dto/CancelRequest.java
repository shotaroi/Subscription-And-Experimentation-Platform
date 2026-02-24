package com.subscriptionplatform.subscription.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        boolean atPeriodEnd
) {}
