# Hábitos QA

Aplicación Android nativa, completamente offline, para seguimiento diario de hábitos y validación del flujo ChatGPT + GitHub + QA Android.

## Funciones

- Crear, editar y eliminar hábitos.
- Marcar y desmarcar cumplimiento del día.
- Progreso diario `X de Y completados`.
- Racha actual: días consecutivos completados empezando por hoy; si hoy está pendiente, la racha es 0.
- Historial de hoy y seis días anteriores.
- Persistencia local con Room.
- Tema claro/oscuro siguiendo Android.

## Identidad

- Application ID: `cl.habitosqa.app`
- versionCode: `1`
- versionName: `1.0.0`
- minSdk: `26`
- targetSdk / compileSdk: `36`

## Stack

Kotlin, Jetpack Compose, Material 3, ViewModel, Repository, Room, Coroutines y Flow. Sin backend, cuentas, analytics ni permiso `INTERNET`.

## Build

Requiere JDK 17 y Android SDK 36.

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

La candidata de QA se genera mediante los workflows exclusivos de esta app en `.github/workflows/`.
