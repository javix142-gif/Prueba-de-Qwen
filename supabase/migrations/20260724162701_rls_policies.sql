-- Primer Año - Row Level Security Policies
-- Migration: 002_rls_policies.sql
-- Description: Enables RLS on all tables and defines access policies

-- ============================================
-- ENABLE ROW LEVEL SECURITY ON ALL TABLES
-- ============================================

ALTER TABLE families ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE shopping_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

-- ============================================
-- PROFILES POLICIES
-- ============================================

-- Users can only view their own profile
CREATE POLICY "Users can view own profile"
  ON profiles FOR SELECT
  TO authenticated
  USING (auth.uid() = id);

-- Users can insert their own profile (on signup)
CREATE POLICY "Users can insert own profile"
  ON profiles FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "Users can update own profile"
  ON profiles FOR UPDATE
  TO authenticated
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

-- ============================================
-- FAMILIES POLICIES
-- ============================================

-- Users can only view families they belong to
CREATE POLICY "Users can view member families"
  ON families FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), id));

-- Only family members can insert (typically done by admin during setup)
-- This policy is restrictive - in practice, family creation should be controlled
CREATE POLICY "Users cannot arbitrarily create families"
  ON families FOR INSERT
  TO authenticated
  WITH CHECK (false);

-- Users cannot update families directly (controlled via admin or owner role)
CREATE POLICY "Users cannot update families"
  ON families FOR UPDATE
  TO authenticated
  USING (false);

-- Users cannot delete families directly
CREATE POLICY "Users cannot delete families"
  ON families FOR DELETE
  TO authenticated
  USING (false);

-- ============================================
-- FAMILY_MEMBERS POLICIES
-- ============================================

-- Users can view membership info for families they belong to
CREATE POLICY "Users can view own family memberships"
  ON family_members FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users cannot insert themselves into families (prevents unauthorized access)
CREATE POLICY "Users cannot self-add to families"
  ON family_members FOR INSERT
  TO authenticated
  WITH CHECK (false);

-- Users cannot modify membership (prevents role escalation)
CREATE POLICY "Users cannot update family memberships"
  ON family_members FOR UPDATE
  TO authenticated
  USING (false);

-- Users cannot delete memberships (controlled by owner/admin)
CREATE POLICY "Users cannot delete family memberships"
  ON family_members FOR DELETE
  TO authenticated
  USING (false);

-- ============================================
-- EVENTS POLICIES
-- ============================================

-- Users can view events from their families
CREATE POLICY "Users can view family events"
  ON events FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can insert events for their families
CREATE POLICY "Users can insert family events"
  ON events FOR INSERT
  TO authenticated
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can update events for their families
CREATE POLICY "Users can update family events"
  ON events FOR UPDATE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id))
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can delete events for their families
CREATE POLICY "Users can delete family events"
  ON events FOR DELETE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- ============================================
-- EXPENSES POLICIES
-- ============================================

-- Users can view expenses from their families
CREATE POLICY "Users can view family expenses"
  ON expenses FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can insert expenses for their families
CREATE POLICY "Users can insert family expenses"
  ON expenses FOR INSERT
  TO authenticated
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can update expenses for their families
CREATE POLICY "Users can update family expenses"
  ON expenses FOR UPDATE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id))
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can delete expenses for their families
CREATE POLICY "Users can delete family expenses"
  ON expenses FOR DELETE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- ============================================
-- PRODUCTS POLICIES
-- ============================================

-- Users can view products from their families
CREATE POLICY "Users can view family products"
  ON products FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can insert products for their families
CREATE POLICY "Users can insert family products"
  ON products FOR INSERT
  TO authenticated
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can update products for their families
CREATE POLICY "Users can update family products"
  ON products FOR UPDATE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id))
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can delete products for their families
CREATE POLICY "Users can delete family products"
  ON products FOR DELETE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- ============================================
-- SHOPPING_ITEMS POLICIES
-- ============================================

-- Users can view shopping items from their families
CREATE POLICY "Users can view family shopping items"
  ON shopping_items FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can insert shopping items for their families
CREATE POLICY "Users can insert family shopping items"
  ON shopping_items FOR INSERT
  TO authenticated
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can update shopping items for their families
CREATE POLICY "Users can update family shopping items"
  ON shopping_items FOR UPDATE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id))
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can delete shopping items for their families
CREATE POLICY "Users can delete family shopping items"
  ON shopping_items FOR DELETE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- ============================================
-- TASKS POLICIES
-- ============================================

-- Users can view tasks from their families
CREATE POLICY "Users can view family tasks"
  ON tasks FOR SELECT
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can insert tasks for their families
CREATE POLICY "Users can insert family tasks"
  ON tasks FOR INSERT
  TO authenticated
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can update tasks for their families
CREATE POLICY "Users can update family tasks"
  ON tasks FOR UPDATE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id))
  WITH CHECK (_private.user_belongs_to_family(auth.uid(), family_id));

-- Users can delete tasks for their families
CREATE POLICY "Users can delete family tasks"
  ON tasks FOR DELETE
  TO authenticated
  USING (_private.user_belongs_to_family(auth.uid(), family_id));

-- ============================================
-- REVOKE PUBLIC ACCESS
-- ============================================

-- Ensure no public access to any table
REVOKE ALL ON TABLE families FROM PUBLIC;
REVOKE ALL ON TABLE profiles FROM PUBLIC;
REVOKE ALL ON TABLE family_members FROM PUBLIC;
REVOKE ALL ON TABLE events FROM PUBLIC;
REVOKE ALL ON TABLE expenses FROM PUBLIC;
REVOKE ALL ON TABLE products FROM PUBLIC;
REVOKE ALL ON TABLE shopping_items FROM PUBLIC;
REVOKE ALL ON TABLE tasks FROM PUBLIC;

-- Revoke execute on private function from public
REVOKE ALL ON FUNCTION _private.user_belongs_to_family(UUID, UUID) FROM PUBLIC;

COMMENT ON POLICY "Users can view member families" ON families IS 'Restricts family visibility to members only';
COMMENT ON POLICY "Users cannot self-add to families" ON family_members IS 'Prevents users from adding themselves to arbitrary families';
COMMENT ON POLICY "Users can view family events" ON events IS 'Events visible only to family members';
COMMENT ON POLICY "Users can view family expenses" ON expenses IS 'Expenses visible only to family members';
COMMENT ON POLICY "Users can view family products" ON products IS 'Products visible only to family members';
COMMENT ON POLICY "Users can view family shopping items" ON shopping_items IS 'Shopping items visible only to family members';
COMMENT ON POLICY "Users can view family tasks" ON tasks IS 'Tasks visible only to family members';
