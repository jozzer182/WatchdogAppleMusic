# AppleMusic Watchdog 🎵

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-Open_Source-blue?style=for-the-badge)

Una aplicación Android que vigila automáticamente el estado de reproducción de Apple Music y reanuda la música si se detiene inesperadamente. Incluye refresh profundo cada 15 minutos para solucionar el congelamiento de Apple Music, optimización de memoria automática y notificaciones con contador en tiempo real.

## 📋 El Problema

Apple Music en Android tiene una tendencia a pausarse o detenerse automáticamente en ciertas situaciones:

- Cuando el dispositivo entra en modo de ahorro de energía
- Después de períodos de inactividad
- Al cambiar entre aplicaciones
- Por optimizaciones agresivas del sistema Android
- **Congelamiento de la app**: A veces Apple Music muestra que está reproduciendo pero no sale audio ni avanza la canción (el problema más frustrante)

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
4. **Refresh profundo automático cada 15 minutos**:
   - ⏸️ Pausa la reproducción actual
   - ⏭️ Salta a la siguiente canción
   - ▶️ Reanuda la reproducción
   - Esto "despierta" a Apple Music cuando se congela mostrando PLAYING sin audio
5. **Control automático de volumen cada 15 minutos**:
   - 🔊 Detecta cuando Android baja el volumen automáticamente
   - 🪟 Lanza una Activity transparente invisible para subir el volumen
   - 🔓 Evita las restricciones de Android para servicios en segundo plano
   - 🔁 Realiza 4 intentos automáticos dentro de la Activity
   - 📢 Mantiene siempre el volumen al máximo para parlantes externos
   - ⚡ Se auto-cierra en 3 segundos sin interrumpir al usuario
6. **Refresh manual a demanda**:
   - 🔄 Botón verde para probar el refresh en cualquier momento
   - Feedback visual en tiempo real con Toast messages mostrando cada paso
7. **Optimización de memoria cada 15 minutos**:
   - Libera procesos en caché innecesarios
   - Ejecuta garbage collector múltiple
   - Limpia archivos temporales antiguos
   - Monitorea estado de memoria del sistema
   - Mejora el rendimiento general del dispositivo
8. **Notificación con contador en tiempo real**:
   - Icono dinámico mostrando minutos restantes hasta próximo refresh
   - Actualización cada segundo del tiempo en formato MM:SS
   - Subtexto con el último estado/acción ejecutada
9. **Servicio foreground persistente**: Evita que Android mate el servicio
10. **Interfaz con countdown**: Visualiza cuándo será el próximo refresh profundo
11. **Sin intervención del usuario**: Todo funciona en segundo plano de forma transparente

### Tecnología Utilizada

- **NotificationListenerService**: Para monitorear el estado de las aplicaciones multimedia
- **MediaSessionManager**: Para acceder a las sesiones de reproducción activas
- **MediaController**: Para enviar comandos de control de reproducción (play, pause, skipToNext)
- **AudioManager**: Para control programático del volumen del sistema
- **ActivityManager**: Para optimizar la memoria y gestionar procesos en segundo plano
- **Foreground Service**: Para mantener el servicio activo con notificación actualizable
- **SharedPreferences**: Para comunicación eficiente entre servicio y UI
- **Canvas/Bitmap API**: Para generar iconos dinámicos monocromáticos en notificaciones
- **Handler/Looper**: Para loops de verificación, refresh y actualización de UI

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

2. **Abre AppleMusic Watchdog** - verás una interfaz con:

   - 📊 Card azul con countdown hasta el próximo refresh
   - 🟣 Botón morado: "Abrir Ajustes de Acceso a Notificaciones"
   - 🟠 Botón naranja: "Desactivar Optimización de Batería"
   - 🔵 Botón azul: "Permitir Notificaciones" (Android 13+)
   - 🟢 Botón verde: "🔄 Probar Refresh Ahora"

3. **Habilita el acceso a notificaciones** (PASO CRÍTICO):

   - Pulsa el botón morado "Abrir Ajustes de Acceso a Notificaciones"
   - Busca "AppleMusic Watchdog" en la lista
   - Activa el interruptor
   - Acepta el permiso cuando se solicite
   - ✅ Verás el countdown aparecer en la app

4. **Desactiva la optimización de batería** (MUY IMPORTANTE):

   - Pulsa el botón naranja "Desactivar Optimización de Batería"
   - Se abrirá directamente el diálogo de solicitud o la lista de apps
   - Busca "AppleMusic Watchdog" si es necesario
   - Selecciona "No optimizar"
   - Esto evita que Android mate el servicio para ahorrar batería

5. **Permite notificaciones** (Android 13+):

   - Pulsa el botón azul "Permitir Notificaciones"
   - Acepta el permiso cuando se solicite
   - Necesario para mostrar la notificación foreground

6. **Prueba el refresh manual** (Opcional):

   - Pulsa el botón verde "🔄 Probar Refresh Ahora"
   - Verás Toast messages mostrando cada paso:
     - 🔍 "Buscando Apple Music..."
     - ⏸️ "Pausando..."
     - ⏭️ "Siguiente canción..."
     - ▶️ "Reanudando..."
     - ✅ "Refresh completado exitosamente"

7. **Revisa la notificación**:

   - Desliza la barra de notificaciones
   - Verás "AppleMusic Watchdog Activo"
   - El icono muestra los **minutos restantes** hasta el próximo refresh
   - El texto muestra el countdown completo en formato MM:SS
   - El subtexto muestra el último estado/acción

8. **¡Listo!** El servicio está vigilando Apple Music automáticamente

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

Una vez configurada, la aplicación funciona completamente en segundo plano.

### Características de la UI

**Pantalla Principal:**

- 📊 **Card de Countdown**: Muestra en tiempo real cuánto falta para el próximo refresh profundo (formato MM:SS)
- 🟣 **Botón Morado**: Acceso rápido a ajustes de notificaciones del sistema
- 🟠 **Botón Naranja**: Desactivar optimización de batería (con múltiples fallbacks para Android 14+)
- 🔵 **Botón Azul**: Solicitar permiso de notificaciones (Android 13+)
- 🟢 **Botón Verde**: Ejecutar refresh manual inmediato con feedback visual

**Notificación Persistente:**

- 🔢 **Icono Dinámico**: Círculo blanco con número mostrando minutos restantes (actualizado cada minuto)
- ⏱️ **Texto Principal**: "Próximo refresh en: MM:SS" (actualizado cada segundo)
- 📝 **Subtexto**: Último estado ejecutado (ej: "✅ Refresh completado exitosamente", "Vigilando Apple Music")
- 🔔 **Notificación Ongoing**: No se puede deslizar para cerrar (garantiza persistencia del servicio)

### Feedback Visual en Tiempo Real

Cuando ejecutas un refresh manual o automático, verás Toast messages mostrando:

- 🔍 "Buscando Apple Music..."
- ⏸️ "Pausando..."
- ⏭️ "Siguiente canción..."
- ▶️ "Reanudando..."
- 🔊 "Restaurando volumen máximo..."
- ⚠️ "Volumen reducido detectado (X/MAX)"
- ✅ "Volumen máximo restaurado"
- ✅ "Refresh completado exitosamente"
- ⚠️ "No hay sesión activa - lanzando Apple Music..."
- ❌ Mensajes de error si algo falla

### Logs de Debugging

Si quieres ver qué está haciendo el servicio en detalle, usa logcat:

```bash
adb logcat -s MediaWatchdogService:D
```

Verás mensajes como:

**Vigilancia regular (cada 60s):**

- `Verificando estado de Apple Music...`
- `Apple Music ya está reproduciendo - no se requiere acción`
- `Apple Music está pausado - intentando reanudar`
- `Enviando comando PLAY a Apple Music`
- `No se encontró sesión activa de Apple Music`

**Refresh profundo (cada 15min o manual):**

- `=== INICIANDO REFRESH PROFUNDO (cada 15 min) ===`
- `🔍 Buscando Apple Music...`
- `Paso 1: Pausando Apple Music`
- `⏸️ Pausando...`
- `Paso 2: Saltando a siguiente canción`
- `⏭️ Siguiente canción...`
- `Paso 3: Reanudando reproducción`
- `▶️ Reanudando...`
- `=== REFRESH COMPLETADO EXITOSAMENTE ===`
- `✅ Refresh completado exitosamente`

**Optimización de memoria:**

- `=== OPTIMIZANDO MEMORIA ===`
- `Memoria total: XXXXmb`
- `Memoria disponible: XXXXmb`
- `Memoria usada: XXXXmb (XX%)`
- `Procesos visibles: XX`
- `  → Matado: com.example.app`
- `  ✗ No se pudo matar: com.example.protected`
- `Procesos en caché eliminados: X`
- `Ejecutando garbage collection...`
- `Memoria disponible después: XXXXmb`
- `Memoria liberada: XXmb`
- `✓ Memoria del sistema en buen estado`
- `⚠️ ADVERTENCIA: Sistema bajo presión de memoria!`
- `=== OPTIMIZACIÓN COMPLETADA ===`

**Control de volumen:**

- `=== FORZANDO VOLUMEN AL MÁXIMO ===`
- `Volumen actual: XX / XX`
- `⚠️ Volumen reducido detectado - iniciando restauración`
- `Intento 1/4: Subiendo volumen a XX`
- `  → Volumen después del intento 1: XX / XX`
- `Intento 2/4: Subiendo volumen a XX`
- `  → Volumen después del intento 2: XX / XX`
- `Intento 3/4: Subiendo volumen a XX`
- `  → Volumen después del intento 3: XX / XX`
- `✅ Volumen restaurado exitosamente en intento 3`
- `✓ Volumen confirmado en máximo: XX / XX`
- `=== VERIFICACIÓN DE VOLUMEN COMPLETADA ===`

**Countdown y notificaciones:**

- `Countdown actualizado: XXX segundos restantes`
- `Countdown broadcast enviado: XXX segundos restantes`

## 🛠️ Arquitectura del Proyecto

```
app/src/main/
├── java/com/zarabandajose/watchdogmusic/
│   ├── MainActivity.kt              # UI con botones y countdown
│   ├── MediaWatchdogService.kt      # Servicio de vigilancia y refresh
│   └── VolumeBoostActivity.kt       # Activity transparente para control de volumen
├── res/
│   ├── layout/
│   │   └── activity_main.xml        # Layout con CardView y 4 botones
│   ├── values/
│   │   ├── strings.xml              # Textos en español
│   │   └── themes_volume.xml        # Tema transparente para VolumeBoostActivity
│   └── mipmap-*/
│       ├── ic_launcher.png          # Icono personalizado en todas las resoluciones
│       └── ic_launcher_round.png    # Icono redondo
└── AndroidManifest.xml              # Configuración de permisos y servicio
```

### Componentes Principales

#### MainActivity

- Muestra countdown visual hasta el próximo refresh
- 4 botones de configuración con colores distintivos
- Botón de test de refresh manual
- Lectura de SharedPreferences para actualizar UI en tiempo real
- Activity Result Launchers para permisos modernos de Android 14+
- Verificación continua del estado del servicio

#### VolumeBoostActivity

- Activity completamente transparente (sin interfaz visible)
- Se lanza automáticamente cuando se detecta volumen reducido
- Realiza 4 intentos de restauración del volumen con delays de 400ms
- Utiliza permisos de Activity en primer plano (evita restricciones de servicios)
- Se auto-cierra después de 3 segundos
- No aparece en apps recientes ni interrumpe al usuario
- Configuración especial: `singleInstance`, `noHistory`, `excludeFromRecents`

#### MediaWatchdogService (NotificationListenerService)

**3 Loops principales:**

1. **checkRunnable**: Verifica estado cada 60s
2. **refreshRunnable**: Ejecuta refresh profundo + control de volumen + optimización cada 15min
3. **countdownUpdateRunnable**: Actualiza countdown y notificación cada 1s

**Funciones clave:**

- `checkAppleMusic()`: Monitoreo del estado de reproducción
- `handleAppleMusicSession()`: Manejo de 11 estados diferentes de PlaybackState
- `performDeepRefresh()`: Secuencia pause → skipToNext → play con broadcasts
- `forceMaxVolume()`: Lanza VolumeBoostActivity transparente para restaurar volumen
- `optimizeMemory()`: Limpieza de memoria con estadísticas detalladas
- `updateCountdownBroadcast()`: Escritura a SharedPreferences + broadcast
- `updateNotification()`: Actualiza notificación foreground con countdown
- `createCountdownIcon()`: Genera ícono monocromático dinámico con Canvas/Bitmap
- `sendStatusBroadcast()`: Envía estado actual para Toast messages
- `onStartCommand()`: Maneja comando de refresh manual

**Características técnicas:**

- Foreground service con notificación actualizable
- Handler/Looper para operaciones asíncronas
- MediaSessionManager para acceso a sesiones de medios
- ActivityManager para gestión de procesos y memoria
- Logs detallados con emojis para debugging visual

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

### Cambiar el Intervalo de Refresh Profundo

Edita `MediaWatchdogService.kt`:

```kotlin
private const val REFRESH_INTERVAL_MS = 900_000L // 15 minutos
```

Cambia el valor según tus necesidades:

- 5 minutos = `300_000L`
- 10 minutos = `600_000L`
- 20 minutos = `1_200_000L`
- 30 minutos = `1_800_000L`

## ⚠️ Limitaciones Conocidas

- Requiere que Apple Music esté instalado (`com.apple.android.music`)
- El servicio puede ser terminado por Android si la optimización de batería está activa
- En algunos dispositivos con optimizaciones agresivas (Xiaomi, Huawei, etc.) puede requerir permisos adicionales de "inicio automático"
- No funciona si el usuario cierra Apple Music manualmente desde el selector de apps recientes

## 🔐 Permisos

La app requiere los siguientes permisos:

- **Acceso a notificaciones** (`BIND_NOTIFICATION_LISTENER_SERVICE`): Para detectar sesiones de medios activas
- **Matar procesos en segundo plano** (`KILL_BACKGROUND_PROCESSES`): Para optimizar la memoria del dispositivo
- **Servicio en primer plano** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`): Para mantener el servicio activo
- **Notificaciones** (`POST_NOTIFICATIONS`): Para mostrar la notificación del servicio foreground
- **Arranque automático** (`RECEIVE_BOOT_COMPLETED`): Para iniciar el servicio al encender el dispositivo

✨ **Todos los permisos son necesarios para el funcionamiento óptimo de la app.**

🔒 No se recopilan datos ni se envía información a servidores externos. Todo funciona localmente en el dispositivo.

## 🐛 Solución de Problemas

### El servicio no parece funcionar

1. ✅ Verifica que el acceso a notificaciones está habilitado (botón morado)
2. 🔋 Desactiva la optimización de batería (botón naranja)
3. 🔄 Reinicia el dispositivo
4. 📱 En MIUI/ColorOS/EMUI: Habilita "Inicio automático" para la app
5. 👁️ Revisa la notificación - debe mostrar el countdown actualizándose

### No veo el countdown en la app

1. El servicio necesita estar habilitado primero (acceso a notificaciones)
2. Verás "Servicio no habilitado" hasta que actives el permiso
3. Después de activar, espera 1-2 segundos para que aparezca el countdown
4. El countdown se actualiza cada segundo

### El botón naranja (batería) no hace nada

En Android 14+ hay múltiples métodos de fallback:

1. Primer intento: Diálogo directo de solicitud
2. Si falla: Lista general de optimización de batería
3. Si falla: Ajustes de la app
4. Revisa los logs con `adb logcat -s MainActivity:D` para ver qué método se usó
5. Si ninguno funciona: Ve manualmente a Ajustes → Apps → AppleMusic Watchdog → Batería → "No optimizar"

### Apple Music no se reanuda automáticamente

1. Abre Apple Music manualmente al menos una vez
2. Reproduce una canción para crear una sesión activa
3. Espera 60 segundos después de pausar para ver si se reanuda
4. Presiona el botón verde para probar refresh manual
5. Revisa los logs con `adb logcat -s MediaWatchdogService:D`

### El servicio se detiene después de un tiempo

- ⚠️ Asegúrate de que la optimización de batería está desactivada (CRÍTICO)
- En algunos dispositivos necesitas bloquear la app en las apps recientes
- Xiaomi MIUI: Ajustes → Apps → Permisos → Inicio automático → Activar
- Huawei EMUI: Ajustes → Aplicaciones → Lanzar → Activar gestión manual
- Samsung: Ajustes → Batería → Apps sin restricciones → Agregar AppleMusic Watchdog

### No veo el icono con números en la notificación

- En Android 14+, los iconos pequeños son monocromáticos (blanco)
- El número aparece dentro de un círculo blanco
- El sistema puede aplicar un tinte según tu tema
- Si solo ves un punto: Fuerza detención de la app y reinicia el servicio
- El icono se actualiza cada vez que cambia el número de minutos

### El refresh manual no muestra mensajes Toast

1. Primero debes habilitar el acceso a notificaciones
2. Si no está habilitado, verás: "⚠️ Primero debes habilitar el acceso a notificaciones"
3. Los Toast aparecen rápidamente uno tras otro mostrando cada paso
4. Duran 2-3 segundos cada uno

### El volumen sigue bajándose automáticamente

1. Verifica que el permiso `MODIFY_AUDIO_SETTINGS` esté concedido (se otorga automáticamente)
2. El servicio sube el volumen cada 15 minutos automáticamente
3. Si necesitas subirlo antes, presiona el botón verde de refresh manual
4. Revisa los logs: `adb logcat -s MediaWatchdogService:D` para ver los intentos de restauración
5. Android hace 4 intentos para romper el bloqueo de protección auditiva
6. Si aún falla, puede ser una restricción del fabricante (MIUI, EMUI, etc.)

## 📄 Licencia

Este proyecto es de código abierto y está disponible para uso personal y educativo.

## 🤝 Contribuciones

¿Tienes ideas para mejorar la app? Las contribuciones son bienvenidas:

- Reporta bugs o problemas
- Sugiere nuevas funcionalidades
- Mejora el código o la documentación

---

**Desarrollado con ❤️ para resolver un problema real de Apple Music en Android**
