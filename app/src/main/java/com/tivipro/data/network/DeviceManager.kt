package com.tivipro.data.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

data class DeviceLicenseStatus(
    val isBlocked: Boolean = false,
    val isPremium: Boolean = false,
    val deviceId: String = ""
)

data class QrCredentialsResult(
    val status: String = "",
    val metodo: String = "",
    val url: String = "",
    val usuario: String = "",
    val password: String = ""
)

object DeviceManager {

    const val MANAGEMENT_SERVER_IP = "84.235.239.152"
    private const val DEFAULT_SERVER_URL = "http://$MANAGEMENT_SERVER_IP/poltivi/api.php"

    // Opcional: Credenciales Basic Auth si la carpeta /admin/ requiere usuario/contraseña
    var basicAuthUser: String? = null
    var basicAuthPass: String? = null

    /**
     * Obtiene o genera el ID único de instalación asociado a este dispositivo (6 caracteres).
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("poltivi_device_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", "")
        if (id.isNullOrBlank() || id.length != 6) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) { null }

            val rawSeed = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                UUID.randomUUID().toString().replace("-", "")
            }

            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val hash = kotlin.math.abs(rawSeed.hashCode().toLong())
            val sb = StringBuilder()
            var seed = hash
            for (i in 0 until 6) {
                val idx = ((seed + i * 31) % chars.length).toInt()
                sb.append(chars[idx])
                seed /= 7
            }
            id = sb.toString().uppercase()
            prefs.edit().putString("device_id", id).apply()
        }
        return id.uppercase()
    }

    private fun parseJsonBoolean(json: JSONObject, vararg keys: String): Boolean {
        for (key in keys) {
            if (!json.has(key)) continue
            val obj = json.opt(key) ?: continue
            if (obj is Boolean) return obj
            if (obj is Number) return obj.toInt() == 1
            val str = obj.toString().trim().lowercase()
            if (str == "true" || str == "1" || str == "yes" || str == "si" || str == "s" || str == "active" || str == "premium") {
                return true
            }
        }
        return false
    }

    /**
     * Verifica la licencia del dispositivo enviando device_id, modelo y versión de OS
     * al servidor web en la IP 84.235.239.152/admin/api.php.
     */
    suspend fun verificarDispositivo(context: Context): DeviceLicenseStatus = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId(context)
        val modelo = "${Build.MANUFACTURER} ${Build.MODEL}"
        val versionOs = Build.VERSION.RELEASE ?: "Android"

        val prefsDevice = context.getSharedPreferences("poltivi_device_prefs", Context.MODE_PRIVATE)
        val prefsSession = context.getSharedPreferences("poltivi_session", Context.MODE_PRIVATE)

        val trialDurationMs = 72 * 3600 * 1000L
        var firstInstallMs = prefsDevice.getLong("first_install_timestamp_ms", 0L)
        if (firstInstallMs == 0L) {
            firstInstallMs = System.currentTimeMillis()
            prefsDevice.edit().putLong("first_install_timestamp_ms", firstInstallMs).apply()
        }
        val isWithinTrial = (System.currentTimeMillis() - firstInstallMs) < trialDurationMs

        val urls = listOf(
            "http://$MANAGEMENT_SERVER_IP/poltivi/api.php",
            "http://$MANAGEMENT_SERVER_IP/admin/api.php",
            "http://$MANAGEMENT_SERVER_IP/api.php"
        )

        for (urlStr in urls) {
            try {
                val client = NetworkModule.getOkHttpClient()
                val formBody = FormBody.Builder()
                    .add("device_id", deviceId)
                    .add("id_dispositivo", deviceId)
                    .add("modelo", modelo)
                    .add("model", modelo)
                    .add("os", versionOs)
                    .add("version_os", versionOs)
                    .add("nombre_app", "poltivi")
                    .add("app", "poltivi")
                    .add("app_name", "poltivi")
                    .add("action", "register")
                    .build()

                val requestBuilder = Request.Builder()
                    .url(urlStr)
                    .post(formBody)
                    .header("User-Agent", "PolTivi-App")
                    .header("X-Device-ID", deviceId)

                val user = basicAuthUser
                val pass = basicAuthPass
                if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
                    requestBuilder.header("Authorization", Credentials.basic(user, pass))
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val responseText = response.body?.string() ?: ""
                    val firstBrace = responseText.indexOf('{')
                    val lastBrace = responseText.lastIndexOf('}')

                    if (firstBrace >= 0 && lastBrace > firstBrace) {
                        val jsonStr = responseText.substring(firstBrace, lastBrace + 1)
                        val json = JSONObject(jsonStr)

                        val serverIsPremium = parseJsonBoolean(json, "es_premium", "premium", "is_premium", "esPremium", "status")
                        val isBlocked = parseJsonBoolean(json, "bloqueado", "blocked", "is_blocked", "isBlocked", "banned")
                        val effectiveIsPremium = serverIsPremium || isWithinTrial

                        prefsDevice.edit().putBoolean("is_premium", effectiveIsPremium).apply()
                        prefsSession.edit().putBoolean("is_premium", effectiveIsPremium).apply()

                        return@withContext DeviceLicenseStatus(
                            isBlocked = isBlocked,
                            isPremium = effectiveIsPremium,
                            deviceId = deviceId
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val savedPremium = prefsDevice.getBoolean("is_premium", false) || prefsSession.getBoolean("is_premium", false) || isWithinTrial
        DeviceLicenseStatus(isBlocked = false, isPremium = savedPremium, deviceId = deviceId)
    }

    /**
     * Consulta al servidor web si se han enviado credenciales para Iniciar Sesión por QR / Remoto.
     * Petición: http://84.235.239.152/poltivi/api.php?device_id=[DEVICE_ID]&tipo_dispositivo=tv
     */
    suspend fun pollQrLoginCredentials(context: Context): QrCredentialsResult? = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId(context)
        val urlApi = "http://$MANAGEMENT_SERVER_IP/poltivi/api.php?device_id=$deviceId&tipo_dispositivo=tv&nombre_app=pol_tivi"

        try {
            val client = NetworkModule.getOkHttpClient()
            val requestBuilder = Request.Builder()
                .url(urlApi)
                .get()
                .header("User-Agent", "PolTivi-App")
                .header("X-Device-ID", deviceId)

            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseText = response.body?.string() ?: ""
                val firstBrace = responseText.indexOf('{')
                val lastBrace = responseText.lastIndexOf('}')

                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    val jsonStr = responseText.substring(firstBrace, lastBrace + 1)
                    val json = JSONObject(jsonStr)

                    val status = json.optString("status", "")
                    if ("success".equals(status, ignoreCase = true) || status.isBlank()) {
                        val metodo = json.optString("metodo", "xtream").trim().lowercase()
                        val url = json.optString("url", json.optString("server_url", json.optString("xtream_url", json.optString("m3u_url", "")))).trim()
                        val usuario = json.optString("usuario", json.optString("username", json.optString("xtream_usuario", ""))).trim()
                        val password = json.optString("password", json.optString("pass", json.optString("xtream_password", ""))).trim()

                        if (url.isNotBlank()) {
                            if (metodo == "xtream" && usuario.isNotBlank()) {
                                return@withContext QrCredentialsResult(
                                    status = "success",
                                    metodo = "xtream",
                                    url = url,
                                    usuario = usuario,
                                    password = password
                                )
                            } else if (metodo == "m3u") {
                                return@withContext QrCredentialsResult(
                                    status = "success",
                                    metodo = "m3u",
                                    url = url,
                                    usuario = "",
                                    password = ""
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Envía la información del dispositivo al servidor web de gestión.
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
                pInfo.versionName ?: "1.6"
            } catch (_: Exception) { "1.6" }

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
