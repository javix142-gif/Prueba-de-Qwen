# Primer Año

Aplicación web familiar compartida para organizar controles, vacunas, citas, tareas, gastos y compras de la bebé.

## Descripción

Primer Año es una PWA (Progressive Web App) diseñada para ser utilizada por dos personas (Javier y Josefina) que comparten la misma información familiar. La aplicación se enfoca en cuatro preguntas clave:

1. ¿Qué tenemos pendiente?
2. ¿Qué viene próximamente?
3. ¿Cuánto hemos gastado?
4. ¿Qué necesitamos comprar?

## Stack Tecnológico

- **Frontend**: React 19 + Vite + JavaScript
- **Backend**: Supabase (PostgreSQL + Auth + Row Level Security)
- **Formato**: Progressive Web App (PWA)
- **Despliegue**: Netlify
- **Tests**: Vitest

**No incluye**: Tailwind, Redux, TypeScript, ni bibliotecas de componentes.

## Estado Actual

**Versión**: 0.1.0  
**Etapa 1 completada**: Base y seguridad ✅

Ver [PROJECT_STATE.md](./PROJECT_STATE.md) para detalles del progreso.

## Instalación y Desarrollo

### Prerrequisitos

- Node.js 18+
- npm
- Cuenta en Supabase (para backend)

### Pasos de Instalación

1. Clonar el repositorio:
```bash
git clone <repo-url>
cd primer-ano
```

2. Instalar dependencias:
```bash
npm install
```

3. Configurar variables de entorno:
```bash
cp .env.example .env
```

Editar `.env` con tus credenciales de Supabase:
```
VITE_SUPABASE_URL=https://tu-proyecto.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=tu-anon-key
```

4. Iniciar servidor de desarrollo:
```bash
npm run dev
```

### Scripts Disponibles

| Comando | Descripción |
|---------|-------------|
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Build de producción |
| `npm run lint` | Linting con oxlint |
| `npm run test` | Tests con vitest |
| `npm run preview` | Preview del build |

## Configuración de Supabase

### Aplicar Migraciones

Las migraciones están en `supabase/migrations/`:

1. `001_initial_schema.sql` - Crea tablas y estructura
2. `002_rls_policies.sql` - Habilita RLS y políticas de seguridad

**Opción A: Supabase Dashboard**
1. Ir al SQL Editor en Supabase Dashboard
2. Copiar y ejecutar cada migración en orden

**Opción B: Supabase CLI**
```bash
supabase link --project-ref tu-project-ref
supabase db push
```

### Setup Manual de Familia

Para pruebas iniciales:

1. Crear una familia en la tabla `families`
2. Crear dos usuarios vía Supabase Authentication
3. Crear perfiles en `profiles` para cada usuario
4. Vincular usuarios a la familia en `family_members`
5. Crear un tercer usuario externo para tests de aislamiento

Ver `supabase/seed.sql` para más detalles.

## Estructura del Proyecto

```
primer-ano/
├── src/
│   ├── lib/            # Configuración de servicios
│   ├── components/     # Componentes reutilizables
│   ├── pages/          # Pantallas principales
│   └── App.jsx         # Componente raíz
├── supabase/
│   ├── migrations/     # Migraciones SQL versionadas
│   ├── tests/database/ # Tests de base de datos
│   └── seed.sql        # Documentación de seed data
├── tests/              # Tests de lógica de negocio
├── public/             # Assets estáticos
├── AGENTS.md           # Instrucciones para agentes IA
├── PROJECT_STATE.md    # Estado actual del proyecto
└── README.md           # Este archivo
```

## Seguridad

La aplicación implementa Row Level Security (RLS) en todas las tablas para garantizar:

- Cada usuario solo ve datos de su familia
- Los usuarios no pueden auto-unirse a familias
- Los usuarios no pueden modificar su membresía o rol
- No hay acceso público a datos familiares

Ver `supabase/migrations/002_rls_policies.sql` para detalles de las políticas.

## Testing

### Tests Unitarios

```bash
npm run test
```

Incluye:
- `tests/calculations.test.js` - Formato de moneda, edad, presupuesto, stock
- `tests/shopping-flow.test.js` - Flujo compra → gasto → stock

### Tests de Base de Datos

Los tests de RLS requieren un proyecto Supabase real:

```bash
# Ejecutar manualmente en Supabase SQL Editor
psql -f supabase/tests/database/test_rls_isolation.sql
```

## Roadmap

### Etapa 1 — Base y seguridad ✅ (Completada)
- Proyecto React + Vite
- Cliente Supabase configurado
- Migraciones iniciales
- Tests básicos

### Etapa 2 — Organización (Pendiente)
- Autenticación
- Asociación a familias
- Pantalla Inicio
- Pantalla Agenda
- Pantalla Pendientes

### Etapa 3 — Dinero y compras (Pendiente)
- Pantalla Gastos
- Inventario
- Lista de compras
- Flujo compra → gasto → stock

### Etapa 4 — Cierre (Pendiente)
- Diseño responsive
- Manifest PWA
- Exportación CSV
- Despliegue

## Contribuciones

Este es un proyecto privado para uso familiar. Para contribuir, contactar a los mantenedores.

## Licencia

Privado. Todos los derechos reservados.

---

**Nota**: Esta aplicación no almacena RUN, información bancaria ni antecedentes clínicos extensos. No emite recomendaciones médicas.
