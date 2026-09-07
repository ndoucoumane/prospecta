-- Migration: V20260907105000__CREATE_CAMPAIGNS_AND_STEPS.sql
-- Description: Create campaigns, campaign_steps, and campaign_prospects tables for sequence automation

CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    channel_strategy VARCHAR(50) NOT NULL DEFAULT 'MULTI_CHANNEL',
    created_by UUID,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaigns_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_campaigns_org_id ON campaigns(organization_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns(status);

CREATE TABLE IF NOT EXISTS campaign_steps (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    position INT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    delay_minutes INT NOT NULL DEFAULT 0,
    subject_template VARCHAR(255),
    content_template TEXT NOT NULL,
    conditions TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaign_steps_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT uq_campaign_steps_campaign_position UNIQUE (campaign_id, position)
);

CREATE INDEX IF NOT EXISTS idx_campaign_steps_campaign_id ON campaign_steps(campaign_id);

CREATE TABLE IF NOT EXISTS campaign_prospects (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    prospect_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_step INT NOT NULL DEFAULT 1,
    next_action_at TIMESTAMP WITH TIME ZONE,
    last_action_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaign_prospects_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_prospects_prospect FOREIGN KEY (prospect_id) REFERENCES prospects(id) ON DELETE CASCADE,
    CONSTRAINT uq_campaign_prospects_campaign_prospect UNIQUE (campaign_id, prospect_id)
);

CREATE INDEX IF NOT EXISTS idx_campaign_prospects_campaign_id ON campaign_prospects(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_prospects_prospect_id ON campaign_prospects(prospect_id);
CREATE INDEX IF NOT EXISTS idx_campaign_prospects_next_action ON campaign_prospects(status, next_action_at);
