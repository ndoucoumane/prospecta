-- Migration: V20260907101000__CREATE_USERS_AND_PROFILES.sql
-- Description: Create user profiles table linked to Keycloak identity and organizations

CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    keycloak_subject VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    job_title VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'SALES_REP',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profiles_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_org_id ON user_profiles(organization_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_keycloak_sub ON user_profiles(keycloak_subject);
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);
