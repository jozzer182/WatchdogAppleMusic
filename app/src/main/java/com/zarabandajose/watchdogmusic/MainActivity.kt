package com.zarabandajose.watchdogmusic

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Configurar botón para abrir ajustes de acceso a notificaciones
        val btnOpenSettings = findViewById<Button>(R.id.btnOpenSettings)
        btnOpenSettings.setOnClickListener {
            openNotificationListenerSettings()
        }
    }
    
    /**
     * Abre la pantalla de ajustes del sistema para habilitar el acceso a notificaciones.
     * El usuario debe activar manualmente esta app en la lista de servicios de notificación.
     */
    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
}