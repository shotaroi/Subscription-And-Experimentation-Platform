# Architecture

## Overview

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                     API Layer (REST)                          │
                    └─────────────────────────────────────────────────────────────┘
                                              │
    ┌─────────────────────────────────────────┼─────────────────────────────────────────┐
    │                                         │                                         │
    ▼                                         ▼                                         ▼
┌───────────────┐                   ┌───────────────┐                   ┌───────────────┐
│  Subscription │                   │    Billing     │                   │ Experimentation│
│    Service    │                   │    Service     │                   │    Service     │
│   (port 8081) │                   │   (port 8082)  │                   │   (port 8083)  │
└───────┬───────┘                   └───────┬───────┘                   └───────┬───────┘
        │                                   │                                   │
        │         ┌─────────────────────────┼─────────────────────────┐         │
        │         │                         │                         │         │
        ▼         ▼                         ▼                         ▼         ▼
┌───────────────┐                   ┌───────────────┐         ┌───────────────┐
│  PostgreSQL   │                   │  PostgreSQL    │         │   Analytics   │
│  (schema:     │                   │  (schema:      │         │    Service     │
│  subscription)│                   │  billing)      │         │   (port 8084)  │
└───────────────┘                   └───────────────┘         └───────┬───────┘
        │                                   │                         │
        │         ┌─────────────────────────┴─────────────────────────┐ │
        │         │                                                 │ │
        ▼         ▼                                                 ▼ ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                              Kafka Topics                                      │
│  subscription.events | billing.events | experiment.events                       │
└───────────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────┐    ┌───────────────┐
│     Redis     │    │  Prometheus   │
│   (cache)     │    │   Grafana    │
└───────────────┘    └───────────────┘
```

## Subscription Lifecycle

```
    FREE ──► TRIALING ──► ACTIVE ──► PAST_DUE ──► GRACE_PERIOD ──► CANCELED / EXPIRED
      │          │          │            │              │
      └──────────┴──────────┴────────────┴──────────────┘
                 (cancel/reactivate transitions)
```

- **FREE:** No paid subscription
- **TRIALING:** In trial period
- **ACTIVE:** Paid, current
- **PAST_DUE:** Payment failed, retrying
- **GRACE_PERIOD:** Max retries exceeded, final grace
- **CANCELED:** User or system canceled
- **EXPIRED:** Period ended without renewal

## Event-Driven (Outbox Pattern)

1. Domain change + outbox row written in **same DB transaction**
2. Background publisher reads unsent outbox rows
3. Publishes to Kafka, marks row as sent
4. At-least-once delivery; consumers must be idempotent

## Database Strategy

Each service uses its **own schema** in a shared PostgreSQL instance:

- `subscription` – subscriptions, outbox
- `billing` – payment intents, idempotency keys, outbox
- `experimentation` – experiments, assignments, outbox
- `analytics` – event_log (exposures, conversions)

## Key Metrics

| Metric | Description |
|--------|-------------|
| `subscriptions_state_transition_total{from,to}` | Subscription state changes |
| `billing_payment_success_total` | Successful payments |
| `billing_payment_failure_total{code}` | Failed payments by failure code |
| `experiments_exposures_total{key,variant}` | Experiment exposures |
| `experiments_conversions_total{key,variant}` | Conversions per variant |
