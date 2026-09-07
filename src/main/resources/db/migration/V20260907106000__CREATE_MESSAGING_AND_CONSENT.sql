-- Migration: V20260907106000__CREATE_MESSAGING_AND_CONSENT.sql
-- Description: Create whatsapp_accounts, communication_consents, messages, and message_deliveries tables

CREATE TABLE IF NOT EXISTS whatsapp_accounts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    business_account_id VARCHAR(100) NOT NULL,
    phone_number_id VARCHAR(100) NOT NULL,
    display_phone_number VARCHAR(50),
    encrypted_access_token TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_whatsapp_accounts_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_whatsapp_accounts_org_id ON whatsapp_accounts(organization_id);
CREATE INDEX IF NOT EXISTS idx_whatsapp_accounts_phone_id ON whatsapp_accounts(phone_number_id);

CREATE TABLE IF NOT EXISTS communication_consents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    prospect_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    source VARCHAR(100),
    obtained_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comm_consents_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_comm_consents_prospect FOREIGN KEY (prospect_id) REFERENCES prospects(id) ON DELETE CASCADE,
    CONSTRAINT uq_comm_consents_prospect_channel UNIQUE (organization_id, prospect_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_comm_consents_prospect ON communication_consents(prospect_id);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    campaign_id UUID,
    prospect_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    direction VARCHAR(50) NOT NULL DEFAULT 'OUTBOUND',
    sender VARCHAR(255),
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    external_id VARCHAR(255),
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL,
    CONSTRAINT fk_messages_prospect FOREIGN KEY (prospect_id) REFERENCES prospects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_org_id ON messages(organization_id);
CREATE INDEX IF NOT EXISTS idx_messages_prospect_id ON messages(prospect_id);
CREATE INDEX IF NOT EXISTS idx_messages_status ON messages(status);
CREATE INDEX IF NOT EXISTS idx_messages_external_id ON messages(external_id);

CREATE TABLE IF NOT EXISTS message_deliveries (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    details TEXT,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_deliveries_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_message_deliveries_msg_id ON message_deliveries(message_id);
