package com.poltivi.data.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

object DeviceManager {

    const val MANAGEMENT_SERVER_IP = "84.235.239.152"
    private const val DEFAULT_SERVER_URL = "http://$MANAGEMENT_SERVER_IP/api/register"

    /**
     * Obtiene o genera el ID único de instalación asociado a este dispositivo.
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("poltivi_device_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", "")
        if (id.isNullOrBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) { null }

            id = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                UUID.randomUUID().toString()
            }
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    /**
     * Envía la información del dispositivo (ID único, modelo, versión de la app)
     * al servidor web de gestión en la IP 84.235.239.152.
     *
     * El servidor web obtiene la IP pública del dispositivo directamente
     * desde la conexión HTTP.
     */
    suspend fun pingManagementServer(
        context: Context,
        customUrl: String = DEFAULT_SERVER_URL
    ): Boolean = withContext(Dispatchers.IO) {
        val targetUrl = customUrl.ifBlank { DEFAULT_SERVER_URL }
        try {
            val client = NetworkModule.getOkHttpClient()
            val deviceId = getDeviceId(context)

            val currentVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "1.4"
            } catch (_: Exception) { "1.4" }

            val jsonBody = JSONObject().apply {
                put("device_id", deviceId)
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("app_version", currentVersion)
                put("timestamp", System.currentTimeMillis())
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .header("User-Agent", "PolTivi-App")
                .header("X-Device-ID", deviceId)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
