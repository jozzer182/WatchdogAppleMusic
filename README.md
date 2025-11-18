# AppleMusic Watchdog 🎵

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-Open_Source-blue?style=for-the-badge)

Una aplicación Android que vigila automáticamente el estado de reproducción de Apple Music y reanuda la música si se detiene inesperadamente.

## 📋 El Problema

Apple Music en Android tiene una tendencia a pausarse o detenerse automáticamente en ciertas situaciones:
- Cuando el dispositivo entra en modo de ahorro de energía
- Después de períodos de inactividad
- Al cambiar entre aplicaciones
- Por optimizaciones agresivas del sistema Android

Esto interrumpe la experiencia de escucha continua, especialmente frustrante cuando se usa para:
- Música de fondo mientras se trabaja o estudia
- Reproducción nocturna para dormir
- Sesiones largas de ejercicio o actividades

## ✨ La Solución

**AppleMusic Watchdog** actúa como un "perro guardián" que vigila constantemente el estado de Apple Music y toma acción automática:

### Funcionamiento
1. **Vigilancia continua**: Cada 60 segundos verifica el estado de reproducción de Apple Music
2. **Detección inteligente**: Identifica si la app está en estado PLAYING, PAUSED, STOPPED, etc.
3. **Acción automática**:
   - Si está pausada/detenida → Envía comando de reproducción (`play()`)
   - Si no hay sesión activa → Lanza la aplicación de Apple Music
4. **Sin intervención del usuario**: Todo funciona en segundo plano de forma transparente

### Tecnología Utilizada
- **NotificationListenerService**: Para monitorear el estado de las aplicaciones multimedia
- **MediaSessionManager**: Para acceder a las sesiones de reproducción activas
- **MediaController**: Para enviar comandos de control de reproducción

## 🚀 Instalación y Configuración

### Requisitos
- Android 8.0 (API 26) o superior
- Apple Music instalado en el dispositivo
- Permisos de acceso a notificaciones

### Instalación desde APK

El APK compilado se encuentra en:
```
app/build/outputs/apk/debug/app-debug.apk
```

O si prefieres la versión release:
```
app/build/outputs/apk/release/app-release.apk
```

### Configuración Paso a Paso

1. **Instala la aplicación** en tu dispositivo Android

2. **Abre AppleMusic Watchdog** y pulsa el botón "Abrir Ajustes de Acceso a Notificaciones"

3. **Habilita el acceso a notificaciones**:
   - Busca "AppleMusic Watchdog" en la lista
   - Activa el interruptor
   - Acepta el permiso cuando se solicite

4. **Desactiva la optimización de batería** (MUY IMPORTANTE):
   - Ve a Ajustes → Batería → Optimización de batería
   - Cambia el filtro a "Todas las apps"
   - Busca "AppleMusic Watchdog"
   - Selecciona "No optimizar"
   
   Esto evita que Android mate el servicio para ahorrar batería.

5. **¡Listo!** El servicio comenzará a vigilar Apple Music automáticamente

## 🔧 Compilar desde el Código Fuente

### Prerrequisitos
- Android Studio Ladybug o superior
- JDK 11 o superior
- Gradle 8.13.1 (incluido)
- Kotlin 2.0.21

### Pasos
```bash
# Clonar o descargar el proyecto
cd WatchdogMusic

# En Windows (PowerShell)
.\gradlew.bat build

# En Linux/Mac
./gradlew build

# El APK estará en:
# app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Uso

Una vez configurada, la aplicación funciona completamente en segundo plano. No necesitas abrirla de nuevo.

### Logs de Debugging
Si quieres ver qué está haciendo el servicio, usa logcat:
```bash
adb logcat -s MediaWatchdogService:D
```

Verás mensajes como:
- `Verificando estado de Apple Music...`
- `Apple Music ya está reproduciendo - no se requiere acción`
- `Apple Music está pausado - intentando reanudar`
- `Enviando comando PLAY a Apple Music`

## 🛠️ Arquitectura del Proyecto

```
app/src/main/
├── java/com/zarabandajose/watchdogmusic/
│   ├── MainActivity.kt              # UI principal
│   └── MediaWatchdogService.kt      # Servicio de vigilancia
├── res/
│   ├── layout/
│   │   └── activity_main.xml        # Layout de la interfaz
│   └── values/
│       └── strings.xml              # Textos de la app
└── AndroidManifest.xml              # Configuración del servicio
```

### Componentes Principales

#### MainActivity
- Muestra instrucciones al usuario
- Botón para abrir ajustes de acceso a notificaciones
- Interfaz simple y clara

#### MediaWatchdogService
- Extiende `NotificationListenerService`
- Verificación cada 60 segundos (configurable en código)
- Manejo de estados: PLAYING, PAUSED, STOPPED, NONE, BUFFERING, ERROR
- Logs detallados para debugging

## ⚙️ Configuración Avanzada

### Cambiar el Intervalo de Verificación

Edita `MediaWatchdogService.kt`:
```kotlin
private const val CHECK_INTERVAL_MS = 60_000L // 60 segundos
```

Cambia el valor a lo que prefieras (en milisegundos):
- 30 segundos = `30_000L`
- 2 minutos = `120_000L`
- 5 minutos = `300_000L`

## ⚠️ Limitaciones Conocidas

- Requiere que Apple Music esté instalado (`com.apple.android.music`)
- El servicio puede ser terminado por Android si la optimización de batería está activa
- En algunos dispositivos con optimizaciones agresivas (Xiaomi, Huawei, etc.) puede requerir permisos adicionales de "inicio automático"
- No funciona si el usuario cierra Apple Music manualmente desde el selector de apps recientes

## 🔐 Permisos

La app solo requiere:
- **Acceso a notificaciones** (`BIND_NOTIFICATION_LISTENER_SERVICE`): Para detectar sesiones de medios activas

No se recopilan datos ni se envía información a servidores externos. Todo funciona localmente en el dispositivo.

## 🐛 Solución de Problemas

### El servicio no parece funcionar
1. Verifica que el acceso a notificaciones está habilitado
2. Desactiva la optimización de batería
3. Reinicia el dispositivo
4. En MIUI/ColorOS: Habilita "Inicio automático" para la app

### Apple Music no se reanuda automáticamente
1. Abre Apple Music manualmente al menos una vez
2. Reproduce una canción para crear una sesión activa
3. Espera 60 segundos después de pausar para ver si se reanuda
4. Revisa los logs con `adb logcat -s MediaWatchdogService:D`

### El servicio se detiene después de un tiempo
- Asegúrate de que la optimización de batería está desactivada
- En algunos dispositivos necesitas bloquear la app en las apps recientes

## 📄 Licencia

Este proyecto es de código abierto y está disponible para uso personal y educativo.

## 🤝 Contribuciones

¿Tienes ideas para mejorar la app? Las contribuciones son bienvenidas:
- Reporta bugs o problemas
- Sugiere nuevas funcionalidades
- Mejora el código o la documentación

---

**Desarrollado con ❤️ para resolver un problema real de Apple Music en Android**
