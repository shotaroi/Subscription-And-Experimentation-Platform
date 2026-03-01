-- Subscription service schema
CREATE SCHEMA IF NOT EXISTS subscription;

CREATE TABLE subscription.subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    plan VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    trial_ends_at TIMESTAMPTZ,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_subscriptions_user_id ON subscription.subscriptions (user_id);
CREATE INDEX idx_subscriptions_status ON subscription.subscriptions (status);
CREATE UNIQUE INDEX idx_subscriptions_user_id_active ON subscription.subscriptions (user_id) WHERE status NOT IN ('CANCELED', 'EXPIRED');

CREATE TABLE subscription.outbox_events (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    aggregate_type VARCHAR(64),
    aggregate_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_sent_at ON subscription.outbox_events (sent_at);
CREATE INDEX idx_outbox_topic ON subscription.outbox_events (topic);
CREATE INDEX idx_outbox_unsent ON subscription.outbox_events (created_at) WHERE sent_at IS NULL;
