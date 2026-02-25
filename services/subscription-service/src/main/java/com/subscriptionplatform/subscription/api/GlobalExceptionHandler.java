package com.subscriptionplatform.subscription.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        String traceId = getTraceId();
        log.warn("Validation failed: {}", errors, ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Validation Failed", 400, "Invalid request body", traceId, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetailResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        String traceId = getTraceId();
        log.warn("Constraint violation: {}", errors, ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Validation Failed", 400, "Invalid request parameters", traceId, errors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetailResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String traceId = getTraceId();
        Map<String, Object> errors = Map.of(ex.getParameterName(), "required");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Missing Parameter", 400, ex.getMessage(), traceId, errors));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalState(IllegalStateException ex) {
        String traceId = getTraceId();
        log.warn("Invalid state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Invalid State", 409, ex.getMessage(), traceId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = getTraceId();
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Bad Request", 400, ex.getMessage(), traceId));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleNotFound(ResourceNotFoundException ex) {
        String traceId = getTraceId();
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Not Found", 404, ex.getMessage(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneric(Exception ex) {
        String traceId = getTraceId();
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetailResponse.of("Internal Server Error", 500, "An unexpected error occurred", traceId));
    }

    private String getTraceId() {
        return org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes() != null
                ? org.slf4j.MDC.get("traceId")
                : null;
    }
}
