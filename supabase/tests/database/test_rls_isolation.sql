-- Primer Año - Database Test Cases
-- File: supabase/tests/database/test_rls_isolation.sql
-- Description: Test cases to verify RLS isolation and access control

-- ============================================
-- TEST SETUP INSTRUCTIONS
-- ============================================
-- These tests require:
-- 1. A Supabase project with migrations applied
-- 2. Three test users created via Supabase Auth:
--    - user_a (member of family_a)
--    - user_b (member of family_a)
--    - user_c (member of family_b, external to family_a)
-- 3. Tests should be run with different auth contexts
--
-- To create test users in Supabase Dashboard:
--   Authentication > Users > Add user
--   Use emails like: test+a@ejemplo.com, test+b@ejemplo.com, test+c@ejemplo.com
--   Do NOT use real personal email addresses
--
-- NOTE: This file documents test cases. Actual execution requires
-- Supabase CLI or psql with appropriate role switching.

-- ============================================
-- TEST CASE 1: RLS IS ENABLED ON ALL TABLES
-- ============================================
-- Uses pg_class.relrowsecurity to check if RLS is enabled
-- Expected: All tables show relrowsecurity = true

SELECT 
  c.relname AS table_name,
  c.relrowsecurity AS rls_enabled
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind = 'r'
  AND c.relname IN ('families', 'profiles', 'family_members', 'events', 'expenses', 'products', 'shopping_items', 'tasks')
ORDER BY c.relname;

-- ============================================
-- TEST CASE 2: POLICIES EXIST FOR ALL TABLES
-- ============================================
-- Uses pg_policy to verify policies are defined
-- Expected: At least one policy per table

SELECT 
  tablename,
  COUNT(*) AS policy_count
FROM pg_policies
WHERE schemaname = 'public'
GROUP BY tablename
ORDER BY tablename;

-- ============================================
-- TEST CASE 3: VERIFY POLICY ROLES
-- ============================================
-- Ensure all policies are for 'authenticated' role only
-- Expected: No policies for 'anon' or 'public'

SELECT 
  tablename,
  policyname,
  roles
FROM pg_policies
WHERE schemaname = 'public'
  AND roles::text NOT LIKE '%authenticated%'
ORDER BY tablename, policyname;
-- Expected result: empty set (all policies should be for authenticated)

-- ============================================
-- TEST CASE 4: VERIFY SECURITY DEFINER FUNCTION
-- ============================================
-- Check that user_belongs_to_family uses SECURITY DEFINER with safe search_path

SELECT 
  proname,
  prosecdef AS is_security_definer,
  proconfig AS search_path_config
FROM pg_proc
WHERE proname = 'user_belongs_to_family';
-- Expected: prosecdef = true, proconfig contains 'search_path='

-- ============================================
-- TEST CASE 5: CHECK CONSTRAINTS ON AMOUNT_CLP
-- ============================================
-- Verify expenses.amount_clp has non-negative constraint

SELECT 
  conname AS constraint_name,
  conrelid::regclass AS table_name,
  pg_get_constraintdef(oid) AS constraint_def
FROM pg_constraint
WHERE conrelid = 'public.expenses'::regclass
  AND contype = 'c';
-- Expected: CHECK constraint with amount_clp >= 0

-- ============================================
-- TEST CASE 6: CHECK CONSTRAINTS ON STOCK
-- ============================================
-- Verify products.current_stock and minimum_stock have non-negative constraints

SELECT 
  conname AS constraint_name,
  conrelid::regclass AS table_name,
  pg_get_constraintdef(oid) AS constraint_def
FROM pg_constraint
WHERE conrelid = 'public.products'::regclass
  AND contype = 'c';
-- Expected: CHECK constraints with current_stock >= 0 and minimum_stock >= 0

-- ============================================
-- INTEGRATION TESTS (require actual data and auth context)
-- ============================================
-- The following tests must be run manually with proper auth setup.
-- They are documented here but commented out to prevent accidental execution.

-- TEST CASE 7: AUTHENTICATED USER CAN ACCESS OWN PROFILE
-- Run as authenticated user_a
-- Expected: Returns user_a's profile
-- SELECT * FROM profiles WHERE id = auth.uid();

-- TEST CASE 8: USER CAN VIEW FAMILIES THEY BELONG TO
-- Run as user_a (member of family_a)
-- Expected: Returns family_a
-- SELECT f.* FROM families f
-- JOIN family_members fm ON f.id = fm.family_id
-- WHERE fm.user_id = auth.uid();

-- TEST CASE 9: USER CANNOT VIEW FAMILIES THEY DON'T BELONG TO
-- Run as user_c (member of family_b, not family_a)
-- Expected: Returns 0 rows for family_a
-- SELECT * FROM families WHERE id = '<family_a_uuid>';

-- TEST CASE 10: USER CANNOT SELF-ADD TO FAMILY
-- Run as user_c trying to add themselves to family_a
-- Expected: INSERT fails due to RLS policy (WITH CHECK false)
-- INSERT INTO family_members (family_id, user_id, role)
-- VALUES ('<family_a_uuid>', '<user_c_uuid>', 'member');

-- TEST CASE 11: USER CANNOT MODIFY FAMILY MEMBERSHIP
-- Run as user_a trying to change their role
-- Expected: UPDATE fails due to RLS policy (USING false)
-- UPDATE family_members SET role = 'owner'
-- WHERE user_id = auth.uid() AND family_id = '<family_a_uuid>';

-- TEST CASE 12: USER CAN INSERT EVENTS FOR THEIR FAMILY
-- Run as user_a
-- Expected: INSERT succeeds for family_a
-- INSERT INTO events (family_id, title, type, event_date, status)
-- VALUES ('<family_a_uuid>', 'Test Event', 'control', '2025-01-01', 'pendiente');

-- TEST CASE 13: USER CANNOT INSERT EVENTS FOR FOREIGN FAMILY
-- Run as user_c trying to insert into family_a
-- Expected: INSERT fails due to RLS policy
-- INSERT INTO events (family_id, title, type, event_date, status)
-- VALUES ('<family_a_uuid>', 'Foreign Event', 'control', '2025-01-01', 'pendiente');

-- TEST CASE 14: EXPENSE AMOUNT CHECK CONSTRAINT
-- Expected: Negative amounts are rejected by CHECK constraint
-- INSERT INTO expenses (family_id, expense_date, description, category, amount_clp, paid_by)
-- VALUES ('<family_a_uuid>', '2025-01-01', 'Test', 'otros', -100, 'javier');

-- TEST CASE 15: STOCK CANNOT BE NEGATIVE
-- Expected: Negative stock values are rejected by CHECK constraint
-- INSERT INTO products (family_id, name, current_stock, minimum_stock, unit)
-- VALUES ('<family_a_uuid>', 'Test Product', -5, 0, 'unidades');

-- TEST CASE 16: ANONYMOUS USER CANNOT ACCESS DATA
-- Run as anonymous (no auth)
-- Expected: All queries return permission denied or empty set
-- SELECT * FROM families;
-- SELECT * FROM events;
-- SELECT * FROM expenses;

-- TEST CASE 17: TWO MEMBERS OF SAME FAMILY SEE SAME DATA
-- Run as user_a: count events for family_a
-- Run as user_b: count events for family_a
-- Expected: Both return the same count
-- SELECT COUNT(*) FROM events WHERE family_id = '<family_a_uuid>';
