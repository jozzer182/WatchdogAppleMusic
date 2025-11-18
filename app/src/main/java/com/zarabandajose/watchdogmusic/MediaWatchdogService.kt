package com.zarabandajose.watchdogmusic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log

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
    }

    private lateinit var mediaSessionManager: MediaSessionManager
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAppleMusic()
            // Volver a ejecutar después del intervalo
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio MediaWatchdogService creado")
        
        // Inicializar MediaSessionManager
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener conectado - iniciando vigilancia de Apple Music")
        
        // Iniciar el loop de verificación
        handler.post(checkRunnable)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener desconectado - deteniendo vigilancia")
        
        // Detener el loop de verificación
        handler.removeCallbacks(checkRunnable)
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
    }
}
