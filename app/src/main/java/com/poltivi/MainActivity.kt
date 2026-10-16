package com.poltivi

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.poltivi.data.network.DeviceManager
import com.poltivi.ui.PolTiviApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (_: Exception) {}
        enableEdgeToEdge()

        // Ping management web server (84.235.239.152) with unique device ID on app startup
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                DeviceManager.pingManagementServer(this@MainActivity)
            } catch (_: Exception) {}
        }

        setContent {
            PolTiviApp(modifier = Modifier.fillMaxSize())
        }
    }
}
