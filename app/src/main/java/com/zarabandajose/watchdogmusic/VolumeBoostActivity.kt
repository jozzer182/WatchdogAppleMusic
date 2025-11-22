package com.zarabandajose.watchdogmusic

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager

/**
 * Activity transparente que se lanza automáticamente para subir el volumen al máximo.
 * Se necesita una Activity porque Android bloquea cambios de volumen desde servicios en segundo plano.
 * 
 * Esta Activity:
 * 1. Se muestra de forma transparente (sin interfaz visible)
 * 2. Sube el volumen al máximo con múltiples intentos
 * 3. Se cierra automáticamente después de 3 segundos
 */
class VolumeBoostActivity : Activity() {

    companion object {
        private const val TAG = "VolumeBoostActivity"
        const val EXTRA_SHOW_TOAST = "show_toast"
    }

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "VolumeBoostActivity iniciada")
        
        // Configurar la ventana para que sea transparente y no interactiva
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        // Ejecutar el boost de volumen
        boostVolume()
        
        // Cerrar automáticamente después de 3 segundos
        handler.postDelayed({
            Log.d(TAG, "Cerrando VolumeBoostActivity")
            finish()
        }, 3000)
    }

    private fun boostVolume() {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            Log.d(TAG, "Volumen actual: $currentVolume / $maxVolume")
            
            if (currentVolume < maxVolume) {
                Log.d(TAG, "⚠️ Volumen reducido - subiendo a máximo desde Activity")
                
                // Realizar 4 intentos con delays cortos
                for (attempt in 1..4) {
                    try {
                        Log.d(TAG, "Intento $attempt/4: Subiendo volumen a $maxVolume")
                        
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            maxVolume,
                            AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                        )
                        
                        Thread.sleep(400)
                        
                        val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        Log.d(TAG, "  → Volumen después del intento $attempt: $newVolume / $maxVolume")
                        
                        if (newVolume == maxVolume) {
                            Log.d(TAG, "✅ Volumen restaurado exitosamente en intento $attempt")
                            break
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en intento $attempt: ${e.message}")
                    }
                }
                
                // Verificación final
                val finalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (finalVolume == maxVolume) {
                    Log.d(TAG, "✓ ÉXITO: Volumen confirmado en máximo: $finalVolume / $maxVolume")
                } else {
                    Log.w(TAG, "⚠️ FALLO: No se alcanzó el máximo: $finalVolume / $maxVolume")
                }
                
            } else {
                Log.d(TAG, "✓ Volumen ya está al máximo")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en boostVolume: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "VolumeBoostActivity destruida")
    }
}
