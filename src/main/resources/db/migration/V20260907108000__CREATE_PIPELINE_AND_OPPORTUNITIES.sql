-- Migration: V20260907108000__CREATE_PIPELINE_AND_OPPORTUNITIES.sql
-- Description: Create opportunities table for Sales Pipeline / CRM Deals

CREATE TABLE IF NOT EXISTS opportunities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    prospect_id UUID NOT NULL,
    company_id UUID,
    assigned_to UUID,
    title VARCHAR(255) NOT NULL,
    stage VARCHAR(50) NOT NULL DEFAULT 'NEW',
    estimated_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'XOF',
    win_probability INT NOT NULL DEFAULT 10,
    expected_close_date DATE,
    closed_at TIMESTAMP WITH TIME ZONE,
    loss_reason VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_opportunities_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_opportunities_prospect FOREIGN KEY (prospect_id) REFERENCES prospects(id) ON DELETE CASCADE,
    CONSTRAINT fk_opportunities_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
    CONSTRAINT fk_opportunities_assigned_to FOREIGN KEY (assigned_to) REFERENCES user_profiles(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_opportunities_org_stage ON opportunities(organization_id, stage);
CREATE INDEX IF NOT EXISTS idx_opportunities_org_prospect ON opportunities(organization_id, prospect_id);
CREATE INDEX IF NOT EXISTS idx_opportunities_org_assigned ON opportunities(organization_id, assigned_to);
CREATE INDEX IF NOT EXISTS idx_opportunities_close_date ON opportunities(expected_close_date);
