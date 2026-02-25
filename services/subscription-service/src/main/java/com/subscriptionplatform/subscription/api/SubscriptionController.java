package com.subscriptionplatform.subscription.api;

import com.subscriptionplatform.subscription.api.dto.CancelRequest;
import com.subscriptionplatform.subscription.api.dto.ReactivateRequest;
import com.subscriptionplatform.subscription.api.dto.StartTrialRequest;
import com.subscriptionplatform.subscription.api.dto.SubscriptionResponse;
import com.subscriptionplatform.subscription.application.SubscriptionApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionApplicationService subscriptionService;

    public SubscriptionController(SubscriptionApplicationService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @RequestParam UUID userId) {
        SubscriptionResponse response = subscriptionService.getSubscriptionForUser(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trial")
    public ResponseEntity<SubscriptionResponse> startTrial(
            @Valid @RequestBody StartTrialRequest request) {
        int trialDays = request.trialDays() > 0 ? request.trialDays() : 14;
        SubscriptionResponse response = subscriptionService.startTrial(request.userId(), trialDays);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(
            @Valid @RequestBody CancelRequest request) {
        SubscriptionResponse response = subscriptionService.cancel(request.userId(), request.atPeriodEnd());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivate(
            @Valid @RequestBody ReactivateRequest request) {
        SubscriptionResponse response = subscriptionService.reactivate(request.userId());
        return ResponseEntity.ok(response);
    }
}
