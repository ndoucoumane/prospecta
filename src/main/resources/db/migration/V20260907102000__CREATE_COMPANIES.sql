-- Migration: V20260907102000__CREATE_COMPANIES.sql
-- Description: Create companies table with tenant isolation and industry attributes

CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    website VARCHAR(255),
    industry VARCHAR(100),
    description TEXT,
    country VARCHAR(10) NOT NULL DEFAULT 'SN',
    city VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    employee_count INT,
    linkedin_url VARCHAR(255),
    facebook_url VARCHAR(255),
    instagram_url VARCHAR(255),
    source VARCHAR(100),
    ai_summary TEXT,
    ai_pain_points TEXT,
    ai_analyzed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_companies_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_companies_org_id ON companies(organization_id);
CREATE INDEX IF NOT EXISTS idx_companies_org_name ON companies(organization_id, name);
CREATE INDEX IF NOT EXISTS idx_companies_website ON companies(website);
