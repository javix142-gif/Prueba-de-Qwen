-- Primer Año - Seed Data Documentation
-- File: supabase/seed.sql
-- Description: Manual setup procedure for test data
--              DO NOT execute this file automatically.
--              Follow the steps below to set up test data manually.

-- ============================================
-- MANUAL SETUP PROCEDURE FOR TESTING
-- ============================================
-- This file documents the steps to create test data.
-- It does NOT contain executable SQL for creating users or families
-- because that requires Supabase Auth and manual intervention.

-- STEP 1: Create a test family
-- ---------------------------------------------
-- In Supabase Dashboard:
--   SQL Editor > New Query
--   Execute:
--   INSERT INTO families (id, name, child_name, child_birth_date, monthly_budget)
--   VALUES ('00000000-0000-0000-0000-000000000001', 'Familia Test A', 'Bebé Test', '2024-06-01', 400000);
--
-- NOTE: Use a generated UUID, not this example UUID.
-- Do NOT use real family names or personal data.

-- STEP 2: Create test users via Supabase Auth
-- ---------------------------------------------
-- In Supabase Dashboard:
--   Authentication > Users > Add user
--   Create three users with these email patterns:
--   - test+a@ejemplo.com (password: TempPass123!)
--   - test+b@ejemplo.com (password: TempPass123!)
--   - test+c@ejemplo.com (password: TempPass123!)
--
-- IMPORTANT: 
-- - Use disposable email addresses or your own domain.
-- - Do NOT use real personal email addresses.
-- - Record the UUID of each user after creation.

-- STEP 3: Create profiles for each user
-- ---------------------------------------------
-- After creating auth users, create their profiles:
--   INSERT INTO profiles (id, display_name)
--   VALUES ('<user_a_uuid>', 'Usuario A');
--   
--   INSERT INTO profiles (id, display_name)
--   VALUES ('<user_b_uuid>', 'Usuario B');
--   
--   INSERT INTO profiles (id, display_name)
--   VALUES ('<user_c_uuid>', 'Usuario C');

-- STEP 4: Create family memberships
-- ---------------------------------------------
-- Link users to families:
--   -- User A and B belong to Family A
--   INSERT INTO family_members (family_id, user_id, role)
--   VALUES ('<family_a_uuid>', '<user_a_uuid>', 'owner');
--   
--   INSERT INTO family_members (family_id, user_id, role)
--   VALUES ('<family_a_uuid>', '<user_b_uuid>', 'member');
--   
--   -- User C belongs to Family B (separate family for isolation testing)
--   INSERT INTO families (id, name, child_name, child_birth_date, monthly_budget)
--   VALUES ('<family_b_uuid>', 'Familia Test B', 'Bebé Test B', '2024-07-01', 300000);
--   
--   INSERT INTO family_members (family_id, user_id, role)
--   VALUES ('<family_b_uuid>', '<user_c_uuid>', 'owner');

-- STEP 5: Verify setup
-- ---------------------------------------------
-- Check that memberships are correct:
--   SELECT f.name AS family, p.display_name AS member, fm.role
--   FROM family_members fm
--   JOIN families f ON f.id = fm.family_id
--   JOIN profiles p ON p.id = fm.user_id
--   ORDER BY f.name, p.display_name;

-- STEP 6: Test RLS isolation
-- ---------------------------------------------
-- Run the tests in supabase/tests/database/test_rls_isolation.sql
-- using different user contexts via Supabase CLI or Dashboard.

-- ============================================
-- WHY THIS IS MANUAL
-- ============================================
-- - User creation requires Supabase Auth (email/password or OAuth).
-- - Passwords cannot be set via SQL for security reasons.
-- - Email verification may be required.
-- - Using seed files with real user data is a security risk.
-- - Each team should create their own test data.

-- ============================================
-- ALTERNATIVE: SUPABASE CLI
-- ============================================
-- If you have Supabase CLI installed, you can use:
--   supabase db reset          # Reset database
--   supabase migration up      # Apply migrations
--   supabase auth users list   # List users
--
-- But user creation still requires Dashboard or Admin API.

