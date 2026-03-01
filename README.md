# Subscription & Experimentation Platform

A production-style, Spotify-like backend platform for subscription lifecycle, billing, and A/B experimentation. Built for portfolio demonstration.

## Tech Stack

- **Java 21** | **Spring Boot 3.3+** | **Maven**
- **PostgreSQL** | **Flyway** | **Spring Data JPA (Hibernate)**
- **Redis** (caching, optional distributed locks)
- **Kafka** (event-driven flows)
- **Micrometer** | **Prometheus** | **Grafana**
- **OpenTelemetry** (basic tracing)
- **Testcontainers** (Postgres, Kafka) for integration tests

## Repository Structure

```
subscription-platform/
├── pom.xml                    # Parent POM
├── services/
│   ├── subscription-service/  # Subscription lifecycle (port 8081)
│   ├── billing-service/       # Payments, idempotency (port 8082)
│   ├── experimentation-service/ # A/B experiments (port 8083)
│   └── analytics-service/     # Event analytics (port 8084)
├── infra/
│   ├── docker-compose.yml     # Local infra
│   ├── prometheus.yml
│   └── grafana/
└── docs/
    ├── architecture.md
    └── runbook.md
```

## Quick Start

### 1. Start Infrastructure

```bash
docker compose -f infra/docker-compose.yml up -d
```

Wait for Postgres, Kafka, Redis, Prometheus, Grafana to be healthy.

### 2. Run Subscription Service

```bash
mvn -pl services/subscription-service spring-boot:run
```

Service runs on **http://localhost:8081**

### 3. Example API Calls

**Start trial:**
```bash
curl -X POST http://localhost:8081/subscriptions/trial \
  -H "Content-Type: application/json" \
  -d '{"userId": "550e8400-e29b-41d4-a716-446655440000", "trialDays": 14}'
```

**Get subscription:**
```bash
curl "http://localhost:8081/subscriptions/me?userId=550e8400-e29b-41d4-a716-446655440000"
```

**Cancel (at period end):**
```bash
curl -X POST http://localhost:8081/subscriptions/cancel \
  -H "Content-Type: application/json" \
  -d '{"userId": "550e8400-e29b-41d4-a716-446655440000", "atPeriodEnd": true}'
```

**Reactivate:**
```bash
curl -X POST http://localhost:8081/subscriptions/reactivate \
  -H "Content-Type: application/json" \
  -d '{"userId": "550e8400-e29b-41d4-a716-446655440000"}'
```

## Running Tests

```bash
# All tests
mvn test

# Subscription service only
mvn -pl services/subscription-service test

# Unit tests only (no containers)
mvn -pl services/subscription-service test -Dtest=SubscriptionStateMachineTest,SubscriptionTest
```

## Observability

- **Actuator:** http://localhost:8081/actuator/health
- **Prometheus metrics:** http://localhost:8081/actuator/prometheus
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)

See [docs/architecture.md](docs/architecture.md) for architecture details and [docs/runbook.md](docs/runbook.md) for on-call troubleshooting.
