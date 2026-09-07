-- Migration: V20260907109500__CREATE_EXTERNAL_EVENTS_AND_IDEMPOTENCY.sql
-- Description: Create external_events and processed_events tables for webhook and Kafka idempotency

CREATE TABLE IF NOT EXISTS external_events (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_external_events_provider_ext_id UNIQUE (provider, external_event_id)
);

CREATE INDEX IF NOT EXISTS idx_external_events_lookup ON external_events(provider, external_event_id);
CREATE INDEX IF NOT EXISTS idx_external_events_processed ON external_events(processed, received_at);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer_name)
);
