package com.subscriptionplatform.subscription.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * RFC 7807 Problem Details for HTTP APIs (application/problem+json).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetailResponse(
        URI type,
        String title,
        int status,
        String detail,
        URI instance,
        Instant timestamp,
        String traceId,
        Map<String, Object> errors
) {
    public static ProblemDetailResponse of(String title, int status, String detail, String traceId) {
        return new ProblemDetailResponse(
                URI.create("https://api.subscription-platform.com/errors/" + status),
                title,
                status,
                detail,
                null,
                Instant.now(),
                traceId,
                null
        );
    }

    public static ProblemDetailResponse of(String title, int status, String detail, String traceId, Map<String, Object> errors) {
        return new ProblemDetailResponse(
                URI.create("https://api.subscription-platform.com/errors/" + status),
                title,
                status,
                detail,
                null,
                Instant.now(),
                traceId,
                errors
        );
    }
}
