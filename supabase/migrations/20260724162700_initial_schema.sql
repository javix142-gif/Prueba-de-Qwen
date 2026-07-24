-- Primer Año - Initial Database Schema
-- Migration: 001_initial_schema.sql
-- Description: Creates base tables for family organization app

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- INFRASTRUCTURE TABLES
-- ============================================

-- Families table: stores family unit information
CREATE TABLE IF NOT EXISTS families (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(255) NOT NULL,
  child_name VARCHAR(255) NOT NULL,
  child_birth_date DATE NOT NULL,
  monthly_budget INTEGER NOT NULL DEFAULT 0 CHECK (monthly_budget >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Profiles table: stores user profile data linked to auth.users
CREATE TABLE IF NOT EXISTS profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  display_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Family members table: links users to families with roles
CREATE TABLE IF NOT EXISTS family_members (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  role VARCHAR(50) NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'member')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(family_id, user_id)
);

-- ============================================
-- FUNCTIONAL TABLES
-- ============================================

-- Events table: controls, vaccines, appointments
CREATE TABLE IF NOT EXISTS events (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL CHECK (type IN ('control', 'vacuna', 'consulta', 'examen', 'tramite', 'otro')),
  event_date DATE NOT NULL,
  event_time TIME,
  location VARCHAR(255),
  status VARCHAR(50) NOT NULL DEFAULT 'pendiente' CHECK (status IN ('pendiente', 'agendado', 'realizado', 'cancelado')),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Expenses table: family expenses tracking
CREATE TABLE IF NOT EXISTS expenses (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  expense_date DATE NOT NULL,
  description VARCHAR(255) NOT NULL,
  category VARCHAR(50) NOT NULL CHECK (category IN ('alimentacion', 'panales', 'higiene', 'salud', 'ropa', 'equipamiento', 'traslado', 'cuidado', 'otros')),
  amount_clp INTEGER NOT NULL CHECK (amount_clp >= 0),
  paid_by VARCHAR(50) NOT NULL CHECK (paid_by IN ('javier', 'josefina')),
  is_planned BOOLEAN NOT NULL DEFAULT true,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Products table: inventory of essential items
CREATE TABLE IF NOT EXISTS products (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  current_stock INTEGER NOT NULL DEFAULT 0 CHECK (current_stock >= 0),
  minimum_stock INTEGER NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
  unit VARCHAR(50) NOT NULL DEFAULT 'unidades',
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Shopping items table: shopping list
CREATE TABLE IF NOT EXISTS shopping_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  product_id UUID REFERENCES products(id) ON DELETE SET NULL,
  description VARCHAR(255) NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  priority VARCHAR(50) NOT NULL DEFAULT 'normal' CHECK (priority IN ('baja', 'normal', 'alta')),
  assigned_to VARCHAR(50) CHECK (assigned_to IN ('javier', 'josefina')),
  status VARCHAR(50) NOT NULL DEFAULT 'pendiente' CHECK (status IN ('pendiente', 'cotizando', 'comprado', 'descartado')),
  purchased_amount INTEGER CHECK (purchased_amount >= 0),
  purchased_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tasks table: pending tasks and paperwork
CREATE TABLE IF NOT EXISTS tasks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL CHECK (type IN ('domestica', 'tramite', 'documento', 'salud', 'compra_especial', 'otro')),
  assigned_to VARCHAR(50) CHECK (assigned_to IN ('javier', 'josefina')),
  due_date DATE,
  priority VARCHAR(50) NOT NULL DEFAULT 'normal' CHECK (priority IN ('baja', 'normal', 'alta', 'urgente')),
  status VARCHAR(50) NOT NULL DEFAULT 'pendiente' CHECK (status IN ('pendiente', 'en_progreso', 'completada', 'cancelada')),
  notes TEXT,
  external_url VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX IF NOT EXISTS idx_family_members_family_id ON family_members(family_id);
CREATE INDEX IF NOT EXISTS idx_family_members_user_id ON family_members(user_id);

CREATE INDEX IF NOT EXISTS idx_events_family_id ON events(family_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);

CREATE INDEX IF NOT EXISTS idx_expenses_family_id ON expenses(family_id);
CREATE INDEX IF NOT EXISTS idx_expenses_expense_date ON expenses(expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);

CREATE INDEX IF NOT EXISTS idx_products_family_id ON products(family_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);

CREATE INDEX IF NOT EXISTS idx_shopping_items_family_id ON shopping_items(family_id);
CREATE INDEX IF NOT EXISTS idx_shopping_items_status ON shopping_items(status);

CREATE INDEX IF NOT EXISTS idx_tasks_family_id ON tasks(family_id);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);

-- ============================================
-- HELPER FUNCTION FOR FAMILY MEMBERSHIP CHECK
-- ============================================

-- Create a secure schema for helper functions
CREATE SCHEMA IF NOT EXISTS _private;

-- Revocar acceso por defecto al esquema privado
REVOKE ALL ON SCHEMA _private FROM PUBLIC;
REVOKE ALL ON SCHEMA _private FROM anon;
REVOKE ALL ON SCHEMA _private FROM authenticated;

-- Function to check if a user belongs to a family
-- This function is security definer to avoid recursion in RLS policies
-- SECURITY NOTE: Uses SET search_path = '' to prevent schema injection attacks
CREATE OR REPLACE FUNCTION _private.user_belongs_to_family(check_user_id UUID, check_family_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.family_members fm
    WHERE fm.user_id = check_user_id AND fm.family_id = check_family_id
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = '';

-- Conceder solo USAGE del esquema (necesario para encontrar la función)
GRANT USAGE ON SCHEMA _private TO authenticated;

-- Conceder solo EXECUTE de la función específica
REVOKE ALL ON FUNCTION _private.user_belongs_to_family(UUID, UUID) FROM PUBLIC;
REVOKE ALL ON FUNCTION _private.user_belongs_to_family(UUID, UUID) FROM anon;
GRANT EXECUTE ON FUNCTION _private.user_belongs_to_family(UUID, UUID) TO authenticated;
-- TRIGGERS FOR UPDATED_AT
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_families_updated_at BEFORE UPDATE ON families
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_events_updated_at BEFORE UPDATE ON events
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_expenses_updated_at BEFORE UPDATE ON expenses
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shopping_items_updated_at BEFORE UPDATE ON shopping_items
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE families IS 'Family unit information';
COMMENT ON TABLE profiles IS 'User profile data linked to auth.users';
COMMENT ON TABLE family_members IS 'Links users to families with roles';
COMMENT ON TABLE events IS 'Controls, vaccines, appointments and other events';
COMMENT ON TABLE expenses IS 'Family expenses tracking in CLP';
COMMENT ON TABLE products IS 'Inventory of essential consumable items';
COMMENT ON TABLE shopping_items IS 'Shopping list with optional inventory integration';
COMMENT ON TABLE tasks IS 'Pending tasks, paperwork and documents to manage';

