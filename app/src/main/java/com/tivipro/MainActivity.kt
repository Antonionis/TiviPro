package com.tivipro

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.tivipro.data.network.DeviceManager
import com.tivipro.data.repository.DataRepositoryImpl
import com.tivipro.ui.PolTiviApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (_: Exception) {}
        enableEdgeToEdge()

        // Llamar a la verificación del dispositivo al abrir la app
        verificarDispositivo()

        setContent {
            PolTiviApp(modifier = Modifier.fillMaxSize())
        }
    }

    fun verificarDispositivo() {
        lifecycleScope.launch {
            val status = DeviceManager.verificarDispositivo(this@MainActivity)
            DataRepositoryImpl(applicationContext).setPremiumStatus(status.isPremium)
            if (status.isBlocked) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Acceso denegado")
                    .setMessage("Este dispositivo ha sido bloqueado.")
                    .setCancelable(false)
                    .setPositiveButton("Salir") { _, _ -> finish() }
                    .show()
            }
        }
    }
}
