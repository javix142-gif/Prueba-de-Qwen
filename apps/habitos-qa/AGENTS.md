# Hábitos QA — reglas para agentes

## Objetivo
App Android offline para seguimiento diario de hábitos y prueba reproducible de QA Android.

## Alcance permitido
- `apps/habitos-qa/**`
- `.github/workflows/habitos-qa-*.yml`

No modificar otros proyectos del repositorio contenedor.

## Stack
- Kotlin 2.2.21
- Android Gradle Plugin 8.13.0
- Gradle 8.13
- JDK 17
- Jetpack Compose + Material 3
- Room 2.8.4
- minSdk 26 / targetSdk 36 / compileSdk 36
- Sin red ni `android.permission.INTERNET`

## Arquitectura
Single Activity + Compose + ViewModel + Repository + `HabitStore`. Room es la fuente de verdad. No agregar Hilt, backend, WorkManager, Firebase ni módulos Gradle adicionales.

## Identidad estable
No cambiar sin decisión explícita:
- `applicationId = cl.habitosqa.app`
- versionCode / versionName de candidata
- esquema Room

## Validación
Orden recomendado:
1. `./gradlew testDebugUnitTest`
2. `./gradlew lintDebug`
3. `./gradlew connectedDebugAndroidTest`
4. `./gradlew assembleDebug`
5. verificar APK, firma, metadatos y ausencia de INTERNET
6. instalación + arranque + logcat + screenshots en emulador
7. `git diff --check` y revisión de alcance

## Seguridad
No secretos, keystores release, servicios externos, permisos innecesarios ni outputs de `build/` en Git.

## Terminado
Funcionalidad solicitada implementada, pruebas relevantes aprobadas, emulador validado, capturas revisadas, APK debug y artefacto QA verificados, sin cambios fuera de alcance.
