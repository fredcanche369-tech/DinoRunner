# Dino Runner — compilación automática

Este proyecto incluye un workflow de GitHub Actions que compila automáticamente
el APK debug.

## Desde el teléfono

1. Crea un repositorio nuevo en GitHub.
2. Sube todos los archivos de esta carpeta al repositorio.
3. Abre la pestaña **Actions**.
4. Selecciona **Build Dino Runner APK**.
5. Pulsa **Run workflow**.
6. Cuando termine correctamente, abre la ejecución y busca **Artifacts**.
7. Descarga **DinoRunner-APK**.
8. Dentro estará `app-debug.apk`.

GitHub Actions ejecuta la compilación en una máquina virtual y permite descargar
el APK como un artifact. El proyecto usa Gradle y Android Gradle Plugin.
