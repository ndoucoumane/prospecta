-- Migration: V20260911100000__ADD_DISCOVERY_AND_LEAD_LISTS.sql
-- Description: Add external_id to prospects and companies, and create lead_lists tables

-- 1. Add external_id to prospects for data provider tracking (Apollo, etc.)
ALTER TABLE prospects ADD COLUMN IF NOT EXISTS external_id VARCHAR(150);
CREATE INDEX IF NOT EXISTS idx_prospects_org_external_id ON prospects(organization_id, external_id);

-- 2. Add external_id to companies
ALTER TABLE companies ADD COLUMN IF NOT EXISTS external_id VARCHAR(150);
CREATE INDEX IF NOT EXISTS idx_companies_org_external_id ON companies(organization_id, external_id);

-- 3. Create lead_lists table (Tenant isolated)
CREATE TABLE IF NOT EXISTS lead_lists (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_lists_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lead_lists_org_id ON lead_lists(organization_id);
CREATE INDEX IF NOT EXISTS idx_lead_lists_org_name ON lead_lists(organization_id, name);

-- 4. Create lead_list_members table (Mapping lead_list <-> prospect)
CREATE TABLE IF NOT EXISTS lead_list_members (
    id UUID PRIMARY KEY,
    lead_list_id UUID NOT NULL,
    prospect_id UUID NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_list_members_list FOREIGN KEY (lead_list_id) REFERENCES lead_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_list_members_prospect FOREIGN KEY (prospect_id) REFERENCES prospects(id) ON DELETE CASCADE,
    CONSTRAINT uq_lead_list_member UNIQUE (lead_list_id, prospect_id)
);

CREATE INDEX IF NOT EXISTS idx_lead_list_members_list_id ON lead_list_members(lead_list_id);
CREATE INDEX IF NOT EXISTS idx_lead_list_members_prospect_id ON lead_list_members(prospect_id);
