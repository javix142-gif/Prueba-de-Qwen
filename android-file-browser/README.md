# Explorador de archivos Android

Aplicación Android nativa para navegar, abrir y compartir archivos de una carpeta seleccionada por el usuario mediante Storage Access Framework.

## Funciones
- Seleccionar una carpeta del teléfono.
- Navegar por subcarpetas.
- Abrir archivos con aplicaciones compatibles.
- Compartir archivos manteniendo presionado.
- Recordar la carpeta autorizada.
- Sin permisos de almacenamiento invasivos.

## Compilación
Requiere JDK 17, Android SDK 36, Build Tools 36.0.0, Gradle 9.5 y Android Gradle Plugin 9.3.1.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Límites de Android
Android 11 o superior restringe el acceso mediante el selector a ciertas raíces, `Download`, `Android/data` y `Android/obb`.
