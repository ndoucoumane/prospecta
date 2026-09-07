-- Migration: V20260907103500__CREATE_ICP_AND_SCORING.sql
-- Description: Create ideal_customer_profiles table for targeted lead scoring

CREATE TABLE IF NOT EXISTS ideal_customer_profiles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    target_industries TEXT,
    target_cities TEXT,
    target_countries TEXT,
    min_employees INT DEFAULT 1,
    max_employees INT DEFAULT 1000,
    target_job_titles TEXT,
    keywords TEXT,
    excluded_industries TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_icp_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_icp_org_id ON ideal_customer_profiles(organization_id);
