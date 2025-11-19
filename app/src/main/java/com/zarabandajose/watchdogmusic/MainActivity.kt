package com.zarabandajose.watchdogmusic

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    
    companion object {
        const val ACTION_COUNTDOWN_UPDATE = "com.zarabandajose.watchdogmusic.COUNTDOWN_UPDATE"
        const val EXTRA_SECONDS_REMAINING = "seconds_remaining"
    }
    
    private lateinit var countdownText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isServiceEnabled = false
    
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.zarabandajose.watchdogmusic.REFRESH_STATUS") {
                val message = intent.getStringExtra("status_message") ?: ""
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado. El servicio foreground puede no funcionar correctamente.", Toast.LENGTH_LONG).show()
        }
    }
    
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Verificar si ahora está exento
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "✓ Optimización de batería desactivada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se desactivó la optimización. Inténtalo manualmente.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private val checkServiceRunnable = object : Runnable {
        override fun run() {
            isServiceEnabled = isNotificationServiceEnabled()
            if (!isServiceEnabled) {
                countdownText.text = "Servicio no habilitado"
            } else {
                // Leer el countdown desde SharedPreferences
                val prefs = getSharedPreferences("watchdog_prefs", Context.MODE_PRIVATE)
                val remainingSeconds = prefs.getLong("countdown_seconds", -1)
                val lastUpdate = prefs.getLong("last_update", 0)
                
                // Verificar que la actualización sea reciente (menos de 5 segundos)
                val now = System.currentTimeMillis()
                if (remainingSeconds >= 0 && (now - lastUpdate) < 5000) {
                    updateCountdown(remainingSeconds)
                } else if (isServiceEnabled) {
                    countdownText.text = "Iniciando..."
                }
            }
            handler.postDelayed(this, 1000) // Actualizar cada segundo
        }
    }
    
    private val countdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_COUNTDOWN_UPDATE) {
                val secondsRemaining = intent.getLongExtra(EXTRA_SECONDS_REMAINING, 0)
                android.util.Log.d("MainActivity", "Countdown update received: $secondsRemaining segundos")
                updateCountdown(secondsRemaining)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Obtener referencia al TextView del contador
        countdownText = findViewById(R.id.countdownText)
        
        // Registrar BroadcastReceiver para recibir actualizaciones del servicio
        val filter = IntentFilter(ACTION_COUNTDOWN_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(countdownReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(countdownReceiver, filter)
        }
        
        // Registrar receiver para estados de refresh
        val statusFilter = IntentFilter("com.zarabandajose.watchdogmusic.REFRESH_STATUS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, statusFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, statusFilter)
        }
        
        // Configurar botón para abrir ajustes de acceso a notificaciones
        val btnOpenSettings = findViewById<Button>(R.id.btnOpenSettings)
        btnOpenSettings.setOnClickListener {
            openNotificationListenerSettings()
        }
        
        // Configurar botón para desactivar optimización de batería
        val btnBatteryOptimization = findViewById<Button>(R.id.btnBatteryOptimization)
        btnBatteryOptimization.setOnClickListener {
            requestBatteryOptimizationExemption()
        }
        
        // Configurar botón para permiso de notificaciones
        val btnNotificationPermission = findViewById<Button>(R.id.btnNotificationPermission)
        btnNotificationPermission.setOnClickListener {
            requestNotificationPermission()
        }
        
        // Configurar botón para probar refresh manual
        val btnTestRefresh = findViewById<Button>(R.id.btnTestRefresh)
        btnTestRefresh.setOnClickListener {
            testRefreshNow()
        }
        
        // Iniciar verificación del estado del servicio
        handler.post(checkServiceRunnable)
    }
    
    /**
     * Abre la pantalla de ajustes del sistema para habilitar el acceso a notificaciones.
     * El usuario debe activar manualmente esta app en la lista de servicios de notificación.
     */
    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    /**
     * Solicita exención de la optimización de batería.
     * CRUCIAL: Sin esto, Android matará el servicio después de un tiempo.
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(
                    this,
                    "Ya está exento de optimización de batería ✓",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            // Para Android 14+, intentar primero el método directo
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                Log.d("MainActivity", "Abriendo ajuste de batería con ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
                batteryOptimizationLauncher.launch(intent)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al abrir optimización directa: ${e.message}")
                
                // Fallback: Abrir la lista general de optimización de batería
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    
                    Log.d("MainActivity", "Abriendo lista general de optimización de batería")
                    batteryOptimizationLauncher.launch(intent)
                    
                    Toast.makeText(
                        this,
                        "Busca 'AppleMusic Watchdog' y selecciona 'No optimizar'",
                        Toast.LENGTH_LONG
                    ).show()
                    
                } catch (e2: Exception) {
                    Log.e("MainActivity", "Error al abrir lista general: ${e2.message}")
                    
                    // Último fallback: Abrir ajustes de la app
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        
                        Toast.makeText(
                            this,
                            "Ve a 'Batería' → 'Optimización de batería' → Busca la app → 'No optimizar'",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e3: Exception) {
                        Toast.makeText(
                            this,
                            "No se pudo abrir ajustes. Configúralo manualmente en Ajustes → Apps → AppleMusic Watchdog → Batería",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    /**
     * Solicita permiso para mostrar notificaciones (Android 13+).
     * Necesario para el foreground service.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(
                        this,
                        "Permiso de notificaciones ya concedido \u2713",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    notificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        } else {
            Toast.makeText(
                this,
                "Permiso no necesario en esta versión de Android",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Ejecuta un refresh manual de Apple Music.
     * Envía un comando al servicio para que ejecute la secuencia completa.
     */
    private fun testRefreshNow() {
        if (!isNotificationServiceEnabled()) {
            Toast.makeText(
                this,
                "\u26A0\uFE0F Primero debes habilitar el acceso a notificaciones",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        Toast.makeText(this, "\uD83D\uDD04 Ejecutando refresh manual...", Toast.LENGTH_SHORT).show()
        
        // Enviar intent al servicio para ejecutar refresh manual
        val intent = Intent(this, MediaWatchdogService::class.java)
        intent.action = "com.zarabandajose.watchdogmusic.MANUAL_REFRESH"
        startService(intent)
    }
    
    /**
     * Actualiza el contador regresivo en la UI.
     */
    private fun updateCountdown(secondsRemaining: Long) {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        countdownText.text = String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Verifica si el servicio de notificaciones está habilitado.
     */
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val packageName = packageName
        return enabledListeners?.contains(packageName) == true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkServiceRunnable)
        try {
            unregisterReceiver(countdownReceiver)
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Receiver ya fue desregistrado
        }
    }
}