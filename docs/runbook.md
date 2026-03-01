# Runbook – On-Call Troubleshooting

## Prerequisites

- Access to Grafana (http://localhost:3000)
- Access to Prometheus (http://localhost:9090)
- Logs with `traceId`, `userId`, `subscriptionId` where relevant

---

## 1. Subscription Service Issues

### Symptom: "Invalid subscription transition" (409)

**Cause:** Client attempted invalid state transition (e.g. CANCELED → TRIALING).

**Action:**
- Check `subscriptions_state_transition_total` in Grafana
- Verify allowed transitions in `SubscriptionStateMachine`
- Return clear error to client with current status

### Symptom: "No subscription found for user" (404)

**Cause:** User has no subscription record.

**Action:**
- Confirm `userId` is correct
- Check `subscription.subscriptions` for the user
- If new user, they must call `POST /subscriptions/trial` first

### Symptom: High latency on `/subscriptions/me`

**Action:**
- Check Redis connectivity (if caching enabled)
- Check Postgres connection pool: `hikaricp_connections_active`
- Review slow query logs

---

## 2. Billing Service Issues

### Symptom: Duplicate payment (idempotency)

**Cause:** Client retried with same `Idempotency-Key`; response should be identical.

**Action:**
- Verify `Idempotency-Key` header is sent
- Check `billing.idempotency_keys` for the key
- Same key must return same response; no duplicate charge

### Symptom: Payment confirmation race (two threads, same payment)

**Cause:** Concurrency; only one should succeed.

**Action:**
- Check unique constraint on `idempotency_key`
- Verify `@Version` / `SELECT FOR UPDATE` on subscription
- One thread gets 409 or constraint violation; that’s expected

### Symptom: `billing_payment_failure_total` spike

**Action:**
- Break down by `code`: INSUFFICIENT_FUNDS, NETWORK_ERROR, etc.
- Check retry schedule (1h, 6h, 24h)
- After max retries, subscription moves to GRACE_PERIOD

---

## 3. Kafka / Outbox Issues

### Symptom: Events not appearing in Kafka

**Action:**
- Check outbox table: `SELECT * FROM subscription.outbox_events WHERE sent_at IS NULL`
- Verify Kafka broker connectivity
- Check service logs for "Failed to publish outbox event"
- Restart outbox publisher; it will retry unsent rows

### Symptom: Duplicate events in Kafka

**Cause:** At-least-once delivery; publisher may retry.

**Action:**
- Consumers must be idempotent (e.g. by event id or idempotency key)
- Check consumer lag in Kafka

---

## 4. Infrastructure

### Postgres

```bash
# Connection check
psql -h localhost -U platform -d subscription_platform -c "SELECT 1"

# Schema check
psql -h localhost -U platform -d subscription_platform -c "\dn"
```

### Redis

```bash
redis-cli -h localhost ping
```

### Kafka

```bash
# List topics
docker exec subscription-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 5. Useful Queries

### Subscription status distribution

```sql
SELECT status, COUNT(*) FROM subscription.subscriptions GROUP BY status;
```

### Recent outbox failures

```sql
SELECT * FROM subscription.outbox_events WHERE sent_at IS NULL ORDER BY created_at DESC LIMIT 10;
```

### Prometheus: subscription transitions

```
rate(subscriptions_state_transition_total[5m])
```
