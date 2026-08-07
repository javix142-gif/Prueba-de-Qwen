# PROJECT STATE — Hábitos QA

## Proyecto
- Nombre: Hábitos QA
- Repositorio: `javix142-gif/Prueba-de-Qwen`
- Ubicación: `apps/habitos-qa/`
- Rama estable: `main`
- Rama candidata: `feat/habitos-qa-mvp`
- Versión: `1.0.0` (`versionCode 1`)

## Objetivo actual
Construir y validar la candidata inicial Android offline, incluyendo QA dinámico en emulador y APK debug verificable.

## Estado
- Implementación MVP: en desarrollo.
- Room: esquema v1.
- Validación CI/QA: pendiente de ejecutar sobre el commit candidato.
- Prueba física: pendiente y fuera de la automatización.

## Decisiones vigentes
- Kotlin + Compose + Material 3 + Room.
- Single Activity, ViewModel, Repository y `HabitStore` para pruebas.
- Sin Dynamic Color.
- Sin INTERNET, backend, cuentas, notificaciones ni permisos especiales.
- Racha: consecutiva hacia atrás desde hoy; si hoy no está completado, racha 0.
- Historial: hoy + seis días previos, solo lectura.

## Riesgos
- La candidata no puede declararse completa hasta verificar Actions, emulador, logcat, capturas y APK real.

## Próximo paso
Ejecutar bootstrap del Gradle Wrapper y después CI/QA Android de la rama candidata.
