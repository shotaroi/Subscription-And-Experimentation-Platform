package com.subscriptionplatform.subscription.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReactivateRequest(
        @NotNull(message = "userId is required")
        UUID userId
) {}
