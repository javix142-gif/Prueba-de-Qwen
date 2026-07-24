# Primer Año - Estado del Proyecto

## Versión Actual: 0.1.0

## Etapa 1 — Base y seguridad ✅ COMPLETADA (AUDITORÍA REALIZADA)

### Fecha de Completación
24 de julio de 2026

### Auditoría Técnica Realizada

Se realizó una auditoría técnica completa de las migraciones y políticas RLS con las siguientes correcciones aplicadas:

#### Correcciones en Migraciones

1. **Renombrado de migraciones**: Se cambió el formato a `<timestamp>_<nombre>.sql` para compatibilidad con Supabase CLI:
   - `20260724162700_initial_schema.sql`
   - `20260724162701_rls_policies.sql`

2. **Función SECURITY DEFINER corregida** (`20260724162700_initial_schema.sql`):
   - Se cambió `SET search_path = public, _private` por `SET search_path = ''` para prevenir ataques de inyección de schema
   - Se agregó prefijo `public.` explícito en la referencia a `family_members` dentro de la función
   - Se agregaron comentarios explicativos sobre la medida de seguridad

3. **Permisos del schema `_private` reforzados**:
   - `REVOKE ALL ON SCHEMA _private FROM PUBLIC;`
   - `REVOKE ALL ON SCHEMA _private FROM authenticated;`
   - La función mantiene `GRANT EXECUTE ... TO authenticated` como único permiso concedido

#### Correcciones en Tests SQL

El archivo `supabase/tests/database/test_rls_isolation.sql` fue completamente reescrito para:

- Usar `pg_class.relrowsecurity` en lugar de `relpolicies IS NOT NULL` para verificar RLS
- Agregar verificación de existencia de políticas mediante `pg_policies`
- Verificar que todas las políticas sean exclusivamente para el rol `authenticated`
- Verificar configuración de SECURITY DEFINER en la función auxiliar
- Verificar CHECK constraints en `expenses.amount_clp` y `products.current_stock`
- Documentar 11 casos de integración adicionales (comentados, requieren auth real)
- Incluir instrucciones explícitas para creación de usuarios de prueba sin datos personales

### Archivos Creados/Modificados en Etapa 1

#### Frontend React
- `src/lib/supabase.js` - Cliente Supabase configurado
- `src/App.jsx` - Pantalla mínima de verificación
- `package.json` - Dependencias instaladas (React, Vite, Supabase)

#### Configuración
- `.env.example` - Plantilla de variables de entorno
- `.gitignore` - Excluye .env y archivos sensibles
- `vite.config.js` - Configuración con Vitest para tests

#### Base de Datos (Supabase)
- `supabase/migrations/20260724162700_initial_schema.sql` - Esquema inicial corregido
  - 8 tablas: families, profiles, family_members, events, expenses, products, shopping_items, tasks
  - UUID como primary key
  - CHECK constraints para valores no negativos
  - Índices en foreign keys
  - Función segura `_private.user_belongs_to_family()` con search_path vacío
  - Triggers para updated_at
  - Permisos reforzados en schema _private

- `supabase/migrations/20260724162701_rls_policies.sql` - Políticas RLS
  - RLS habilitado en las 8 tablas
  - Políticas SELECT/INSERT/UPDATE/DELETE por familia
  - Prevención de auto-unión a familias (WITH CHECK false)
  - Prevención de modificación de membresía (USING false)
  - Revocación de acceso público

- `supabase/seed.sql` - Documentación de setup manual (sin datos reales)
- `supabase/tests/database/test_rls_isolation.sql` - Tests de RLS corregidos

#### Tests
- `tests/calculations.test.js` - 10 tests passing (cálculos básicos)
- `tests/shopping-flow.test.js` - 8 tests passing (flujo compras)

#### Documentación
- `AGENTS.md` - Instrucciones para agentes de IA
- `README.md` - Documentación completa
- `PROJECT_STATE.md` - Este archivo

### Validaciones Ejecutadas

| Validación | Resultado | Comando | Notas |
|------------|-----------|---------|-------|
| Build producción | ✅ Exitoso | `npm run build` | 191.23 kB JS, 4.10 kB CSS |
| Lint | ✅ 0 errores | `npm run lint` | 6 archivos, 91 reglas |
| Tests unitarios | ✅ 18/18 passing | `npm run test -- --run` | 2 test files |
| Sintaxis SQL | ⚠️ No verificada | - | Requiere psql o Supabase CLI |
| Ejecución migraciones | ⏳ Pendiente | - | Requiere instancia Supabase |
| Tests RLS integración | ⏳ Pendiente | - | Requiere instancia Supabase con datos |

### Pruebas Pendientes de Base de Datos

Las siguientes pruebas requieren un proyecto Supabase real con las migraciones aplicadas:

1. **RLS habilitado**: Verificar `relrowsecurity = true` en las 8 tablas
2. **Políticas existen**: Al menos 1 política por tabla en `pg_policies`
3. **Roles correctos**: Todas las políticas solo para `authenticated`
4. **Función segura**: `prosecdef = true` y `proconfig` con `search_path=`
5. **CHECK constraints**: Verificar constraints en expenses y products
6. **Usuario miembro**: Un usuario puede ver datos de su familia
7. **Segundo miembro**: Otro usuario de la misma familia ve los mismos datos
8. **Usuario externo**: Un usuario de otra familia NO ve datos ajenos
9. **Usuario anónimo**: Sin autenticación, no hay acceso a datos
10. **Auto-unión bloqueada**: INSERT en family_members es rechazado
11. **Modificación de rol bloqueada**: UPDATE en family_members es rechazado
12. **Valores negativos rechazados**: amount_clp y stock no aceptan negativos

### Comandos para Ejecutar Tests de BD (Pendientes)

```bash
# Requiere Supabase CLI instalado y proyecto linkado
supabase db reset                    # Resetear base de datos local
supabase migration up                # Aplicar migraciones
psql -h localhost -p 54322 -U postgres -d postgres  # Conectar a DB local

# Ejecutar tests estáticos de estructura
psql -f supabase/tests/database/test_rls_isolation.sql

# Tests manuales con usuarios reales (ver instrucciones en el archivo SQL)
```

### Limitaciones Conocidas

1. **Sin Supabase local**: Docker no está disponible en este entorno, por lo que no se pudo ejecutar Supabase localmente para verificar las migraciones.

2. **Sin proyecto remoto**: No se creó ni vinculó ningún proyecto Supabase real. Las migraciones están listas para ser aplicadas manualmente.

3. **Setup manual requerido**: La creación de familias y usuarios debe hacerse manualmente vía Supabase Dashboard siguiendo las instrucciones en `supabase/seed.sql`.

4. **Sin autenticación implementada**: El frontend aún no tiene pantallas de login/signup. Esto será parte de la Etapa 2.

5. **Tests SQL no ejecutados**: Los tests de `test_rls_isolation.sql` están documentados pero no fueron ejecutados contra una base de datos real.

### Decisiones de Seguridad Implementadas (Auditadas)

1. **Función SECURITY DEFINER con search_path seguro**: `_private.user_belongs_to_family()` usa `SET search_path = ''` para prevenir ataques de inyección de schema. La función referencia explícitamente `public.family_members`.

2. **Schema privado con permisos revocados**: 
   - Schema `_private` creado para funciones auxiliares
   - `REVOKE ALL ON SCHEMA _private FROM PUBLIC`
   - `REVOKE ALL ON SCHEMA _private FROM authenticated`
   - Solo la función tiene `GRANT EXECUTE ... TO authenticated`

3. **Políticas restrictivas en family_members**:
   - INSERT: `WITH CHECK (false)` - nadie puede auto-unirse
   - UPDATE: `USING (false)` - nadie puede modificar membresía
   - DELETE: `USING (false)` - nadie puede eliminar membresías

4. **Políticas restrictivas en families**:
   - INSERT/UPDATE/DELETE: `false` - control administrativo externo

5. **Sin service_role en frontend**: El cliente Supabase usa solo la anon key (`VITE_SUPABASE_PUBLISHABLE_KEY`).

6. **Credenciales excluidas de Git**: `.env`, `.env.local`, `.env.*` en `.gitignore`.

### Estructura Final del Proyecto

```
/workspace/
├── src/
│   ├── lib/
│   │   └── supabase.js
│   ├── assets/
│   ├── App.jsx
│   ├── App.css
│   ├── main.jsx
│   └── index.css
├── supabase/
│   ├── migrations/
│   │   ├── 20260724162700_initial_schema.sql
│   │   └── 20260724162701_rls_policies.sql
│   ├── tests/
│   │   └── database/
│   │       └── test_rls_isolation.sql
│   └── seed.sql
├── tests/
│   ├── calculations.test.js
│   └── shopping-flow.test.js
├── public/
├── .env.example
├── .gitignore
├── AGENTS.md
├── PROJECT_STATE.md
├── README.md
├── package.json
├── package-lock.json
├── vite.config.js
├── index.html
└── node_modules/
```

## Próximo Paso: Etapa 2 — Organización

### Tareas Pendientes

1. **Autenticación**
   - [ ] Pantalla de login/signup
   - [ ] Integración con Supabase Auth
   - [ ] Protección de rutas privadas

2. **Asociación a Familias**
   - [ ] Formulario de creación de familia (primer usuario)
   - [ ] Mecanismo para vincular segundo usuario (código compartido)
   - [ ] Mostrar estado de membresía

3. **Pantalla Inicio**
   - [ ] Edad actual de la bebé
   - [ ] Próximo evento
   - [ ] Gasto acumulado del mes
   - [ ] Presupuesto disponible
   - [ ] Productos bajo stock mínimo
   - [ ] Tareas vencidas o próximas

4. **Pantalla Agenda**
   - [ ] Lista de eventos
   - [ ] Crear/editar/eliminar evento
   - [ ] Cambiar estado de evento
   - [ ] Filtrar por tipo

5. **Pantalla Pendientes**
   - [ ] Lista de tareas
   - [ ] Crear/editar/eliminar tarea
   - [ ] Marcar como completada
   - [ ] Asignar responsable

### Criterios de Aceptación Etapa 2

- [ ] Usuario puede crear cuenta y loguearse
- [ ] Primer usuario crea una familia
- [ ] Segundo usuario se vincula a la familia existente
- [ ] Ambos usuarios ven los mismos datos
- [ ] CRUD de eventos funcional
- [ ] CRUD de tareas funcional
- [ ] Inicio muestra información accionable
- [ ] Tests de integración para autenticación

---

*Última actualización: 24 de julio de 2026 (Auditoría técnica completada)*
