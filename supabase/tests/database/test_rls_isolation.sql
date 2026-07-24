-- Primer Año - Database Test Cases
-- File: supabase/tests/database/test_rls_isolation.sql
-- Description: Static tests to verify RLS structure and constraints
--              Integration tests require Supabase project with auth setup

-- ============================================
-- TEST 1: RLS IS ENABLED ON ALL TABLES
-- ============================================
-- Uses pg_class.relrowsecurity to check if RLS is enabled
-- Expected: All tables show relrowsecurity = true

SELECT 
  c.relname AS table_name,
  CASE WHEN c.relrowsecurity THEN 'YES' ELSE 'NO' END AS rls_enabled,
  CASE 
    WHEN c.relrowsecurity THEN true 
    ELSE false 
  END AS test_passed
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind = 'r'
  AND c.relname IN ('families', 'profiles', 'family_members', 'events', 'expenses', 'products', 'shopping_items', 'tasks')
ORDER BY c.relname;

-- ============================================
-- TEST 2: POLICIES EXIST FOR ALL TABLES
-- ============================================
-- Uses pg_policies to verify policies are defined
-- Expected: At least one policy per table

SELECT 
  tablename,
  COUNT(*) AS policy_count,
  CASE WHEN COUNT(*) > 0 THEN true ELSE false END AS test_passed
FROM pg_policies
WHERE schemaname = 'public'
GROUP BY tablename
ORDER BY tablename;

-- ============================================
-- TEST 3: VERIFY POLICY ROLES
-- ============================================
-- Ensure all policies are for 'authenticated' role only
-- Expected: No policies for 'anon' or 'public'

SELECT 
  tablename,
  policyname,
  roles,
  CASE 
    WHEN roles::text LIKE '%authenticated%' AND roles::text NOT LIKE '%anon%' AND roles::text NOT LIKE '%public%'
    THEN true 
    ELSE false 
  END AS test_passed
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;

-- Policies for non-authenticated roles should return empty set
SELECT 
  'FAIL: Non-authenticated policies found' AS test_result,
  tablename,
  policyname,
  roles
FROM pg_policies
WHERE schemaname = 'public'
  AND roles::text NOT LIKE '%authenticated%';
-- Expected: empty set

-- ============================================
-- TEST 4: VERIFY SECURITY DEFINER FUNCTION
-- ============================================
-- Check that user_belongs_to_family uses SECURITY DEFINER with safe search_path

SELECT 
  proname AS function_name,
  prosecdef AS is_security_definer,
  proconfig AS search_path_config,
  CASE 
    WHEN prosecdef AND proconfig IS NOT NULL AND proconfig::text LIKE '%search_path=%'
    THEN true 
    ELSE false 
  END AS test_passed
FROM pg_proc
WHERE proname = 'user_belongs_to_family';
-- Expected: prosecdef = true, proconfig contains 'search_path='

-- ============================================
-- TEST 5: CHECK CONSTRAINTS ON EXPENSES.AMOUNT_CLP
-- ============================================
-- Verify expenses.amount_clp has non-negative constraint

SELECT 
  conname AS constraint_name,
  conrelid::regclass AS table_name,
  pg_get_constraintdef(oid) AS constraint_def,
  CASE 
    WHEN pg_get_constraintdef(oid) LIKE '%amount_clp >= 0%'
    THEN true 
    ELSE false 
  END AS test_passed
FROM pg_constraint
WHERE conrelid = 'public.expenses'::regclass
  AND contype = 'c';
-- Expected: CHECK constraint with amount_clp >= 0

-- ============================================
-- TEST 6: CHECK CONSTRAINTS ON PRODUCTS STOCK
-- ============================================
-- Verify products.current_stock and minimum_stock have non-negative constraints

SELECT 
  conname AS constraint_name,
  conrelid::regclass AS table_name,
  pg_get_constraintdef(oid) AS constraint_def,
  CASE 
    WHEN pg_get_constraintdef(oid) LIKE '%current_stock >= 0%' 
     AND pg_get_constraintdef(oid) LIKE '%minimum_stock >= 0%'
    THEN true 
    ELSE false 
  END AS test_passed
FROM pg_constraint
WHERE conrelid = 'public.products'::regclass
  AND contype = 'c';
-- Expected: CHECK constraints with current_stock >= 0 and minimum_stock >= 0

-- ============================================
-- TEST 7: VERIFY _private SCHEMA PERMISSIONS
-- ============================================
-- Check that _private schema exists and has restricted permissions

SELECT 
  nspname AS schema_name,
  CASE 
    WHEN nspname = '_private' THEN 'exists'
    ELSE 'other'
  END AS status
FROM pg_namespace
WHERE nspname = '_private';
-- Expected: one row with nspname = '_private'

-- ============================================
-- TEST 8: VERIFY FUNCTION PERMISSIONS
-- ============================================
-- Check execute permissions on user_belongs_to_family

SELECT 
  p.proname AS function_name,
  has_function_privilege('authenticated', p.oid, 'EXECUTE') AS authenticated_can_execute,
  has_function_privilege('public', p.oid, 'EXECUTE') AS public_can_execute,
  has_function_privilege('anon', p.oid, 'EXECUTE') AS anon_can_execute,
  CASE 
    WHEN has_function_privilege('authenticated', p.oid, 'EXECUTE') 
     AND NOT has_function_privilege('public', p.oid, 'EXECUTE')
     AND NOT has_function_privilege('anon', p.oid, 'EXECUTE')
    THEN true 
    ELSE false 
  END AS test_passed
FROM pg_proc p
JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE p.proname = 'user_belongs_to_family'
  AND n.nspname = '_private';
-- Expected: authenticated=true, public=false, anon=false

-- ============================================
-- TEST 9: VERIFY SCHEMA USAGE PERMISSIONS
-- ============================================
-- Check that authenticated has USAGE on _private schema

SELECT 
  n.nspname AS schema_name,
  has_schema_privilege('authenticated', n.oid, 'USAGE') AS authenticated_has_usage,
  has_schema_privilege('public', n.oid, 'USAGE') AS public_has_usage,
  has_schema_privilege('anon', n.oid, 'USAGE') AS anon_has_usage,
  CASE 
    WHEN has_schema_privilege('authenticated', n.oid, 'USAGE')
     AND NOT has_schema_privilege('public', n.oid, 'USAGE')
     AND NOT has_schema_privilege('anon', n.oid, 'USAGE')
    THEN true 
    ELSE false 
  END AS test_passed
FROM pg_namespace n
WHERE n.nspname = '_private';
-- Expected: authenticated=true, public=false, anon=false

-- ============================================
-- SUMMARY QUERIES
-- ============================================

-- Count tables with RLS enabled
SELECT 
  COUNT(*) AS total_tables,
  SUM(CASE WHEN relrowsecurity THEN 1 ELSE 0 END) AS tables_with_rls,
  CASE 
    WHEN COUNT(*) = SUM(CASE WHEN relrowsecurity THEN 1 ELSE 0 END)
    THEN 'PASS: All tables have RLS enabled'
    ELSE 'FAIL: Some tables missing RLS'
  END AS result
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind = 'r'
  AND c.relname IN ('families', 'profiles', 'family_members', 'events', 'expenses', 'products', 'shopping_items', 'tasks');

-- Count policies by command type
SELECT 
  cmd_type,
  COUNT(*) AS policy_count
FROM (
  SELECT 
    CASE 
      WHEN qual IS NOT NULL AND with_check IS NOT NULL THEN 'ALL'
      WHEN qual IS NOT NULL THEN 'SELECT/DELETE'
      WHEN with_check IS NOT NULL THEN 'INSERT/UPDATE'
      ELSE 'UNKNOWN'
    END AS cmd_type
  FROM pg_policy
  WHERE polrelid IN (
    SELECT oid FROM pg_class 
    WHERE relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
  )
) subq
GROUP BY cmd_type;

