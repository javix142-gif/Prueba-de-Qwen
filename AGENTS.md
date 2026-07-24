# Primer Año - Instrucciones para Agentes

## Propósito del Proyecto

Primer Año es una aplicación web familiar compartida para organizar:
- Controles, vacunas y citas
- Tareas y trámites familiares
- Gastos de la bebé
- Compras pendientes
- Stock de productos esenciales

Usuarios: Javier y Josefina (ambos ven y modifican la misma información)

## Stack Tecnológico

- **Frontend**: React + Vite + JavaScript
- **Backend**: Supabase (PostgreSQL + Auth + RLS)
- **Formato**: PWA (Progressive Web App)
- **Despliegue**: Netlify
- **Sin**: Tailwind, Redux, TypeScript, bibliotecas de componentes

## Estructura del Proyecto

```
primer-ano/
├── src/
│   ├── components/     # Componentes reutilizables
│   ├── pages/          # Pantallas principales
│   ├── lib/            # Configuración de servicios
│   └── App.jsx         # Componente raíz
├── supabase/
│   ├── migrations/     # Migraciones SQL versionadas
│   └── tests/database/ # Tests de base de datos
├── tests/              # Tests de lógica de negocio
└── public/             # Assets estáticos
```

## Flujo de Desarrollo por Etapas

### Etapa 1 — Base y seguridad (COMPLETADA ✓)
- [x] Crear proyecto React con Vite
- [x] Configurar cliente Supabase
- [x] Crear migraciones iniciales (tablas + RLS)
- [x] Configurar tests básicos
- [x] Verificar build y lint

### Etapa 2 — Organización (PENDIENTE)
- [ ] Implementar autenticación
- [ ] Vincular usuarios a familias
- [ ] Pantalla Inicio (resumen)
- [ ] Pantalla Agenda (eventos)
- [ ] Pantalla Pendientes (tareas)

### Etapa 3 — Dinero y compras (PENDIENTE)
- [ ] Pantalla Gastos
- [ ] Presupuesto y totales
- [ ] Inventario de productos
- [ ] Lista de compras
- [ ] Flujo "comprado → gasto → stock"

### Etapa 4 — Cierre (PENDIENTE)
- [ ] Diseño responsive
- [ ] Manifest PWA
- [ ] Estados vacíos y errores
- [ ] Exportación CSV
- [ ] Despliegue en Netlify

## Reglas de Seguridad Obligatorias

1. **RLS en todas las tablas**: Cada tabla debe tener Row Level Security habilitado
2. **Aislamiento por familia**: Los usuarios solo ven datos de su familia
3. **No auto-unirse**: Los usuarios no pueden agregarse a familias arbitrariamente
4. **No modificar membresía**: El rol y pertenencia no son editables por usuarios
5. **Función segura**: `user_belongs_to_family()` usa SECURITY DEFINER con search_path fijo

## Modelo de Datos

### Tablas de Infraestructura
- `families`: Unidad familiar (nombre, hijo, fecha nacimiento, presupuesto)
- `profiles`: Datos del usuario (vinculado a auth.users)
- `family_members`: Relación usuario-familia con rol

### Tablas Funcionales (todas con family_id)
- `events`: Controles, vacunas, consultas
- `expenses`: Gastos en CLP (monto entero, no negativo)
- `products`: Inventario de consumibles
- `shopping_items`: Lista de compras
- `tasks`: Tareas y trámites

## Convenciones de Código

### JavaScript
- Usar funciones flecha cuando sea apropiado
- Preferir const sobre let
- Nombres descriptivos en español para dominios, inglés para utilidades técnicas

### CSS
- CSS propio, sin frameworks
- Mantener estilos mínimos y funcionales
- Mobile-first

### Base de Datos
- UUID como clave primaria
- Todos los campos monetarios como INTEGER (pesos chilenos)
- CHECK constraints para valores no negativos
- Índices en foreign keys y campos de búsqueda

## Credenciales y Variables de Entorno

**NUNCA** commitar credenciales reales. Usar únicamente:
- `.env.example` con valores placeholder
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`

**NUNCA** usar service_role key, secret keys o contraseñas en el frontend.

## Procedimiento Manual para Setup de Familia

Para pruebas locales, crear manualmente en Supabase Dashboard:

1. **Crear familia**: INSERT en `families` con datos de prueba
2. **Crear usuarios**: Via Supabase Authentication (emails de prueba)
3. **Crear perfiles**: INSERT en `profiles` con los UUID de auth
4. **Vincular miembros**: INSERT en `family_members` para cada usuario
5. **Usuario externo**: Crear tercer usuario SIN vincular a la familia para tests de aislamiento

## Criterios de Aceptación del MVP

La aplicación estará terminada cuando:
- [ ] Javier y Josefina puedan iniciar sesión por separado
- [ ] Ambos vean los mismos registros familiares
- [ ] Un usuario externo no pueda acceder a esos registros
- [ ] Se puedan CRUD eventos, tareas, gastos, productos y compras
- [ ] Los cálculos de presupuesto y stock sean correctos
- [ ] El flujo compra→gasto→stock funcione sin duplicación
- [ ] La interfaz funcione en celular
- [ ] Sea instalable como PWA
- [ ] No almacene RUN, info bancaria ni antecedentes clínicos
- [ ] No emita recomendaciones médicas
- [ ] Existan tests para cálculos y flujo de compras

## Comandos Disponibles

```bash
npm run dev      # Servidor de desarrollo
npm run build    # Build de producción
npm run lint     # Linting con oxlint
npm run test     # Tests con vitest
npm run preview  # Preview del build
```

## Recursos

- Documentación Supabase: https://supabase.com/docs
- React Docs: https://react.dev
- Vite Docs: https://vitejs.dev
- PWA MDN: https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps
