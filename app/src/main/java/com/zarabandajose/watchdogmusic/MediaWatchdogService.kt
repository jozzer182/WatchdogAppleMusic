package com.zarabandajose.watchdogmusic

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Servicio que vigila el estado de reproducción de Apple Music en Android.
 * 
 * Funcionalidad:
 * - Cada 60 segundos verifica si Apple Music está reproduciendo música.
 * - Si está pausado, detenido o en otro estado que no sea PLAYING, intenta reanudar.
 * - Si no hay sesión activa de Apple Music, intenta lanzar la app.
 * 
 * IMPORTANTE: El usuario debe:
 * 1. Habilitar esta app en "Acceso a notificaciones" desde los ajustes del sistema.
 * 2. Desactivar la optimización de batería para esta app para evitar que Android
 *    mate el servicio con demasiada agresividad.
 */
class MediaWatchdogService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaWatchdogService"
        private const val APPLE_MUSIC_PACKAGE = "com.apple.android.music"
        private const val CHECK_INTERVAL_MS = 60_000L // 60 segundos
        private const val REFRESH_INTERVAL_MS = 900_000L // 15 minutos
        private const val NOTIFICATION_CHANNEL_ID = "watchdog_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_COUNTDOWN_UPDATE = "com.zarabandajose.watchdogmusic.COUNTDOWN_UPDATE"
        private const val EXTRA_SECONDS_REMAINING = "seconds_remaining"
        const val ACTION_MANUAL_REFRESH = "com.zarabandajose.watchdogmusic.MANUAL_REFRESH"
        const val ACTION_REFRESH_STATUS = "com.zarabandajose.watchdogmusic.REFRESH_STATUS"
        const val EXTRA_STATUS_MESSAGE = "status_message"
    }

    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var activityManager: ActivityManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var lastRefreshTime = 0L
    private var lastStatusMessage = ""
    
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAppleMusic()
            // Volver a ejecutar después del intervalo
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }
    
    private val refreshRunnable = object : Runnable {
        override fun run() {
            performDeepRefresh()
            forceMaxVolume()
            optimizeMemory()
            // Volver a ejecutar después de 15 minutos
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }
    
    private val countdownUpdateRunnable = object : Runnable {
        override fun run() {
            updateCountdownBroadcast()
            // Actualizar cada segundo
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio MediaWatchdogService creado")
        
        // Inicializar servicios del sistema
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Promover a foreground service para evitar que Android lo mate
        startForegroundService()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener conectado - iniciando vigilancia de Apple Music")
        
        // Iniciar los loops de verificación, refresh y countdown
        handler.post(checkRunnable)
        handler.post(refreshRunnable)
        handler.post(countdownUpdateRunnable)
        
        lastRefreshTime = System.currentTimeMillis()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener desconectado - deteniendo vigilancia")
        
        // Detener todos los loops
        handler.removeCallbacks(checkRunnable)
        handler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(countdownUpdateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MANUAL_REFRESH -> {
                Log.d(TAG, "Refresh manual solicitado desde UI")
                sendStatusBroadcast("\uD83D\uDD04 Iniciando refresh manual...")
                handler.post {
                    performDeepRefresh()
                    forceMaxVolume()
                    optimizeMemory()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Verifica el estado de Apple Music y toma acciones según el estado encontrado.
     */
    private fun checkAppleMusic() {
        try {
            Log.d(TAG, "Verificando estado de Apple Music...")
            
            // Obtener el ComponentName de este servicio
            val componentName = ComponentName(this, MediaWatchdogService::class.java)
            
            // Obtener todas las sesiones de medios activas
            val activeSessions = mediaSessionManager.getActiveSessions(componentName)
            
            // Buscar la sesión de Apple Music
            val appleMusicController = activeSessions.find { controller ->
                controller.packageName == APPLE_MUSIC_PACKAGE
            }
            
            if (appleMusicController == null) {
                // No hay sesión activa de Apple Music
                Log.d(TAG, "No se encontró sesión activa de Apple Music")
                launchAppleMusic()
            } else {
                // Hay sesión de Apple Music, verificar el estado de reproducción
                handleAppleMusicSession(appleMusicController)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar Apple Music: ${e.message}", e)
        }
    }

    /**
     * Maneja la sesión de Apple Music encontrada, verificando su estado y actuando según sea necesario.
     */
    private fun handleAppleMusicSession(controller: MediaController) {
        val playbackState = controller.playbackState
        
        if (playbackState == null) {
            Log.d(TAG, "Apple Music: estado de reproducción es null, intentando reanudar")
            resumePlayback(controller)
            return
        }
        
        when (playbackState.state) {
            PlaybackState.STATE_PLAYING -> {
                Log.d(TAG, "Apple Music ya está reproduciendo - no se requiere acción")
            }
            
            PlaybackState.STATE_PAUSED -> {
                Log.d(TAG, "Apple Music está pausado - intentando reanudar")
                resumePlayback(controller)
            }
            
            PlaybackState.STATE_STOPPED -> {
                Log.d(TAG, "Apple Music está detenido - intentando reanudar")
                resumePlayback(controller)
            }
            
            PlaybackState.STATE_NONE -> {
                Log.d(TAG, "Apple Music sin estado de reproducción - intentando reanudar")
                resumePlayback(controller)
            }
            
            PlaybackState.STATE_BUFFERING -> {
                Log.d(TAG, "Apple Music está bufferizando - intentando reanudar")
                resumePlayback(controller)
            }
            
            PlaybackState.STATE_ERROR -> {
                Log.d(TAG, "Apple Music está en estado de error - intentando reanudar")
                resumePlayback(controller)
            }
            
            PlaybackState.STATE_CONNECTING -> {
                Log.d(TAG, "Apple Music está conectando - esperando...")
            }
            
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> {
                Log.d(TAG, "Apple Music está en transición (estado: ${playbackState.state}) - no se requiere acción")
            }
            
            else -> {
                Log.d(TAG, "Apple Music en estado desconocido: ${playbackState.state} - intentando reanudar")
                resumePlayback(controller)
            }
        }
    }

    /**
     * Intenta reanudar la reproducción usando MediaController.
     */
    private fun resumePlayback(controller: MediaController) {
        try {
            Log.d(TAG, "Enviando comando PLAY a Apple Music")
            controller.transportControls.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error al intentar reanudar reproducción: ${e.message}", e)
        }
    }

    /**
     * Intenta lanzar la app de Apple Music cuando no hay sesión activa.
     */
    private fun launchAppleMusic() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(APPLE_MUSIC_PACKAGE)
            
            if (launchIntent != null) {
                Log.d(TAG, "Lanzando app de Apple Music")
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Log.e(TAG, "No se pudo obtener intent de lanzamiento para Apple Music - ¿está instalada la app?")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al intentar lanzar Apple Music: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Servicio MediaWatchdogService destruido")
        handler.removeCallbacks(checkRunnable)
        handler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(countdownUpdateRunnable)
    }
    
    /**
     * Inicia el servicio en foreground para que Android no lo mate.
     */
    private fun startForegroundService() {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AppleMusic Watchdog Activo")
            .setContentText("Vigilando Apple Music y optimizando el rendimiento")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Servicio promovido a foreground")
    }
    
    /**
     * Crea el canal de notificaciones necesario para Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Watchdog Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el servicio de vigilancia de Apple Music activo"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Realiza un refresh profundo de Apple Music cada 15 minutos.
     * Esto soluciona el problema de congelamiento donde la app dice estar en PLAY
     * pero no reproduce audio ni avanza la canción.
     */
    private fun performDeepRefresh() {
        try {
            Log.d(TAG, "=== INICIANDO REFRESH PROFUNDO (cada 15 min) ===")
            sendStatusBroadcast("\uD83D\uDD0D Buscando Apple Music...")
            
            val componentName = ComponentName(this, MediaWatchdogService::class.java)
            val activeSessions = mediaSessionManager.getActiveSessions(componentName)
            val appleMusicController = activeSessions.find { it.packageName == APPLE_MUSIC_PACKAGE }
            
            if (appleMusicController != null) {
                val playbackState = appleMusicController.playbackState
                Log.d(TAG, "Estado actual antes de refresh: ${playbackState?.state}")
                
                // Secuencia: PAUSE → espera → NEXT → espera → PLAY
                Log.d(TAG, "Paso 1: Pausando Apple Music")
                sendStatusBroadcast("⏸️ Pausando...")
                appleMusicController.transportControls.pause()
                
                // Esperar 1 segundo
                Thread.sleep(1000)
                
                Log.d(TAG, "Paso 2: Saltando a siguiente canción")
                sendStatusBroadcast("⏭️ Siguiente canción...")
                appleMusicController.transportControls.skipToNext()
                
                // Esperar 1.5 segundos para que cargue la siguiente canción
                Thread.sleep(1500)
                
                Log.d(TAG, "Paso 3: Reanudando reproducción")
                sendStatusBroadcast("▶️ Reanudando...")
                appleMusicController.transportControls.play()
                
                Log.d(TAG, "=== REFRESH COMPLETADO EXITOSAMENTE ===")
                sendStatusBroadcast("✅ Refresh completado exitosamente")
                lastRefreshTime = System.currentTimeMillis()
            } else {
                Log.d(TAG, "No hay sesión de Apple Music para refresh - intentando lanzar app")
                sendStatusBroadcast("⚠️ No hay sesión activa - lanzando Apple Music...")
                launchAppleMusic()
                Thread.sleep(2000)
                sendStatusBroadcast("\uD83D\uDCF1 Apple Music lanzada")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante refresh profundo: ${e.message}", e)
            sendStatusBroadcast("❌ Error: ${e.message}")
        }
    }
    
    /**
     * Optimiza la memoria del dispositivo liberando recursos.
     * Esto ayuda a mantener Apple Music funcionando sin problemas.
     * 
     * NOTA: Desde Android 5.1, runningAppProcesses está limitado por privacidad,
     * pero podemos liberar memoria de nuestra propia app y hacer optimizaciones generales.
     */
    private fun optimizeMemory() {
        try {
            Log.d(TAG, "=== OPTIMIZANDO MEMORIA ===")
            
            // Obtener información de memoria antes de la optimización
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val availableMemBefore = memoryInfo.availMem / (1024 * 1024) // MB
            val totalMemory = memoryInfo.totalMem / (1024 * 1024) // MB
            val usedMemory = totalMemory - availableMemBefore
            val memoryPercent = (usedMemory.toFloat() / totalMemory.toFloat() * 100).toInt()
            
            Log.d(TAG, "Memoria total: ${totalMemory}MB")
            Log.d(TAG, "Memoria disponible: ${availableMemBefore}MB")
            Log.d(TAG, "Memoria usada: ${usedMemory}MB ($memoryPercent%)")
            
            // 1. Intentar matar procesos en caché visibles (limitado por el sistema)
            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            var killedCount = 0
            
            Log.d(TAG, "Procesos visibles: ${runningApps.size}")
            
            for (process in runningApps) {
                // Intentar matar procesos en caché (excepto Apple Music y procesos del sistema)
                if (process.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
                    && process.processName != APPLE_MUSIC_PACKAGE
                    && !process.processName.startsWith("com.android")
                    && !process.processName.startsWith("android")
                    && !process.processName.startsWith("com.google")
                    && process.processName != packageName) {
                    
                    try {
                        activityManager.killBackgroundProcesses(process.processName)
                        killedCount++
                        Log.d(TAG, "  → Matado: ${process.processName}")
                    } catch (e: Exception) {
                        Log.d(TAG, "  ✗ No se pudo matar: ${process.processName} - ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "Procesos en caché eliminados: $killedCount")
            
            // 2. Forzar garbage collection múltiples veces para mayor efectividad
            Log.d(TAG, "Ejecutando garbage collection...")
            System.runFinalization()
            System.gc()
            Thread.sleep(100)
            Runtime.getRuntime().gc()
            Thread.sleep(100)
            System.gc()
            
            // 3. Liberar caches de esta app
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < System.currentTimeMillis() - (24 * 60 * 60 * 1000)) {
                        file.delete() // Borrar archivos de caché de más de 24 horas
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "No se pudo limpiar caché: ${e.message}")
            }
            
            // 4. Verificar memoria después de la optimización
            Thread.sleep(500) // Esperar a que se libere la memoria
            activityManager.getMemoryInfo(memoryInfo)
            val availableMemAfter = memoryInfo.availMem / (1024 * 1024) // MB
            val freed = availableMemAfter - availableMemBefore
            
            Log.d(TAG, "Memoria disponible después: ${availableMemAfter}MB")
            Log.d(TAG, "Memoria liberada: ${freed}MB")
            
            // 5. Verificar si el sistema está bajo presión de memoria
            if (memoryInfo.lowMemory) {
                Log.w(TAG, "⚠️ ADVERTENCIA: Sistema bajo presión de memoria!")
                Log.w(TAG, "   Umbral bajo: ${memoryInfo.threshold / (1024 * 1024)}MB")
            } else {
                Log.d(TAG, "✓ Memoria del sistema en buen estado")
            }
            
            Log.d(TAG, "=== OPTIMIZACIÓN COMPLETADA ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante optimización de memoria: ${e.message}", e)
        }
    }
    
    /**
     * Fuerza el volumen al máximo con múltiples intentos.
     * Android bloquea subir el volumen después de bajarlo automáticamente por seguridad auditiva.
     * Necesita varios intentos (4x) con delays para romper el bloqueo.
     */
    private fun forceMaxVolume() {
        try {
            Log.d(TAG, "=== FORZANDO VOLUMEN AL MÁXIMO ===")
            sendStatusBroadcast("🔊 Restaurando volumen máximo...")
            
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            Log.d(TAG, "Volumen actual: $currentVolume / $maxVolume")
            
            if (currentVolume < maxVolume) {
                Log.d(TAG, "⚠️ Volumen reducido detectado - iniciando restauración")
                sendStatusBroadcast("⚠️ Volumen reducido detectado ($currentVolume/$maxVolume)")
                
                // Intentar 4 veces con delays para romper el bloqueo de Android
                for (attempt in 1..4) {
                    try {
                        Log.d(TAG, "Intento $attempt/4: Subiendo volumen a $maxVolume")
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            maxVolume,
                            AudioManager.FLAG_SHOW_UI // Mostrar UI para confirmar visualmente
                        )
                        
                        // Esperar un poco entre intentos
                        Thread.sleep(500)
                        
                        val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        Log.d(TAG, "  → Volumen después del intento $attempt: $newVolume / $maxVolume")
                        
                        if (newVolume == maxVolume) {
                            Log.d(TAG, "✅ Volumen restaurado exitosamente en intento $attempt")
                            sendStatusBroadcast("✅ Volumen máximo restaurado")
                            break
                        }
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Error en intento $attempt: ${e.message}")
                    }
                }
                
                // Verificación final
                val finalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (finalVolume < maxVolume) {
                    Log.w(TAG, "⚠️ No se pudo restaurar volumen completamente: $finalVolume / $maxVolume")
                    sendStatusBroadcast("⚠️ Volumen parcialmente restaurado ($finalVolume/$maxVolume)")
                } else {
                    Log.d(TAG, "✓ Volumen confirmado en máximo: $finalVolume / $maxVolume")
                }
                
            } else {
                Log.d(TAG, "✓ Volumen ya está al máximo - no se requiere acción")
            }
            
            Log.d(TAG, "=== VERIFICACIÓN DE VOLUMEN COMPLETADA ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante ajuste de volumen: ${e.message}", e)
            sendStatusBroadcast("❌ Error al ajustar volumen")
        }
    }
    
    /**
     * Envía un broadcast con el estado actual de la operación.
     */
    private fun sendStatusBroadcast(message: String) {
        lastStatusMessage = message
        
        val intent = Intent(ACTION_REFRESH_STATUS)
        intent.putExtra(EXTRA_STATUS_MESSAGE, message)
        sendBroadcast(intent)
        
        // También guardarlo en SharedPreferences
        val prefs = getSharedPreferences("watchdog_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_status", message)
            putLong("last_status_time", System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Envía un broadcast con el tiempo restante hasta el próximo refresh.
     */
    private fun updateCountdownBroadcast() {
        val currentTime = System.currentTimeMillis()
        val elapsedSinceLastRefresh = currentTime - lastRefreshTime
        val remainingMs = REFRESH_INTERVAL_MS - elapsedSinceLastRefresh
        val remainingSeconds = (remainingMs / 1000).coerceAtLeast(0)
        
        // Guardar en SharedPreferences para que MainActivity pueda leerlo
        val prefs = getSharedPreferences("watchdog_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("countdown_seconds", remainingSeconds)
            putLong("last_update", currentTime)
            apply()
        }
        
        // También enviar broadcast por si acaso
        val intent = Intent(ACTION_COUNTDOWN_UPDATE)
        intent.putExtra(EXTRA_SECONDS_REMAINING, remainingSeconds)
        sendBroadcast(intent)
        
        // Actualizar la notificación con el countdown
        updateNotification(remainingSeconds)
        
        Log.d(TAG, "Countdown actualizado: $remainingSeconds segundos restantes")
    }
    
    /**
     * Actualiza la notificación foreground con el countdown actual.
     */
    private fun updateNotification(remainingSeconds: Long) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val countdownText = String.format("%02d:%02d", minutes, seconds)
        
        // Crear icono dinámico con los minutos
        val icon = createCountdownIcon(minutes.toInt())
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AppleMusic Watchdog Activo")
            .setContentText("Próximo refresh en: $countdownText")
            .setSubText(if (lastStatusMessage.isNotEmpty()) lastStatusMessage else "Vigilando Apple Music")
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Crea un icono dinámico mostrando los minutos restantes.
     */
    private fun createCountdownIcon(minutes: Int): IconCompat {
        val size = 64 // Tamaño más pequeño y optimizado para Android
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // En Android, los small icons deben ser monocromáticos (blanco/transparente)
        // El sistema aplicará el tinte automáticamente
        
        // Configurar paint para el círculo de fondo (blanco)
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Dibujar círculo de fondo
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f - 2f
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        
        // Configurar paint para el borde del círculo (para mejor definición)
        val strokePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(centerX, centerY, radius - 1.5f, strokePaint)
        
        // Configurar paint para el texto (transparente - recortar del blanco)
        val textPaint = Paint().apply {
            color = Color.TRANSPARENT
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            // Ajustar tamaño de fuente según cantidad de dígitos
            textSize = if (minutes < 10) 36f else 28f
            // Usar modo de composición para "recortar" del fondo
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        
        // Dibujar el número de minutos
        val text = minutes.toString()
        val textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, centerX, textY, textPaint)
        
        return IconCompat.createWithBitmap(bitmap)
    }
}
