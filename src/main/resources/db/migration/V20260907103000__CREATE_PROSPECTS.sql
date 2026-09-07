-- Migration: V20260907103000__CREATE_PROSPECTS.sql
-- Description: Create prospects table with deduplication indexes and lead scoring fields

CREATE TABLE IF NOT EXISTS prospects (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    company_id UUID,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    full_name VARCHAR(255),
    job_title VARCHAR(150),
    company_name VARCHAR(255),
    company_website VARCHAR(255),
    email VARCHAR(255),
    email_status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    phone VARCHAR(50),
    phone_status VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    whatsapp_number VARCHAR(50),
    country VARCHAR(10) NOT NULL DEFAULT 'SN',
    city VARCHAR(100),
    region VARCHAR(100),
    industry VARCHAR(100),
    company_size VARCHAR(50),
    linkedin_url VARCHAR(255),
    facebook_url VARCHAR(255),
    instagram_url VARCHAR(255),
    source VARCHAR(100) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    lead_score INT NOT NULL DEFAULT 0,
    lead_score_level VARCHAR(50) NOT NULL DEFAULT 'LOW',
    lead_score_reasons TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prospects_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_prospects_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_prospects_org_id ON prospects(organization_id);
CREATE INDEX IF NOT EXISTS idx_prospects_org_email ON prospects(organization_id, email);
CREATE INDEX IF NOT EXISTS idx_prospects_org_phone ON prospects(organization_id, phone);
CREATE INDEX IF NOT EXISTS idx_prospects_org_whatsapp ON prospects(organization_id, whatsapp_number);
CREATE INDEX IF NOT EXISTS idx_prospects_org_status ON prospects(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_prospects_company_id ON prospects(company_id);
