-- Migration: V20260912100000__CREATE_COMMERCIAL_TASKS.sql
-- Description: Create commercial_tasks table for Commercial Tasks module

CREATE TABLE IF NOT EXISTS commercial_tasks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'call',
    title VARCHAR(255) NOT NULL,
    prospect_id UUID,
    prospect_name VARCHAR(255),
    company_name VARCHAR(255),
    phone VARCHAR(100),
    due_date VARCHAR(50) NOT NULL,
    due_time VARCHAR(20) DEFAULT '09:00',
    assigned_to VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commercial_tasks_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_commercial_tasks_prospect FOREIGN KEY (prospect_id) REFERENCES prospects(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_commercial_tasks_org_id ON commercial_tasks(organization_id);
CREATE INDEX IF NOT EXISTS idx_commercial_tasks_org_status ON commercial_tasks(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_commercial_tasks_org_due_date ON commercial_tasks(organization_id, due_date);
CREATE INDEX IF NOT EXISTS idx_commercial_tasks_prospect_id ON commercial_tasks(prospect_id);
