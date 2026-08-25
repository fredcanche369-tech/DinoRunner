# Dino Runner

Juego 2D sencillo para Android, hecho en Java sin librerías externas.

## Requisitos
- Android Studio reciente
- JDK 17 (Android Studio normalmente lo incluye)
- Conexión a Internet la primera vez para descargar Gradle y el plugin de Android.

## Compilar
1. Abre la carpeta `DinoRunner` en Android Studio.
2. Espera a que termine Gradle Sync.
3. Ve a `Build > Build APK(s)`.
4. El APK de debug aparecerá en:
   `app/build/outputs/apk/debug/app-debug.apk`

Para instalarlo en un teléfono, copia ese APK al dispositivo y ábrelo.

## Controles
- Toca la pantalla para saltar.
- Botón de sonido arriba a la derecha.
- La puntuación sube automáticamente.
- La mejor puntuación se guarda con SharedPreferences.
