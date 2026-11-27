package com.tivipro.data.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String
)

object UpdateManager {

    private const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/Antonionis/TiviPro/releases/latest"

    private fun isVersionGreater(serverVer: String, currentVer: String): Boolean {
        val sParts = serverVer.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = currentVer.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(sParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val s = sParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (s > c) return true
            if (s < c) return false
        }
        return false
    }

    suspend fun checkForUpdate(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val client = NetworkModule.getOkHttpClient()

        val currentVersionName = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.8"
        } catch (_: Exception) {
            "1.8"
        }

        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            8
        }

        // Try 1: Primary version.json from GitHub main branch
        try {
            val rawVersionUrl = "https://raw.githubusercontent.com/Antonionis/TiviPro/main/version.json"
            val request = Request.Builder()
                .url(rawVersionUrl)
                .header("User-Agent", "TiviPro-App")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body.string()
                    val json = JSONObject(jsonStr)

                    val serverVersionName = json.optString("versionName", json.optString("version_name", "")).removePrefix("v").trim()
                    val serverVersionCode = json.optInt("versionCode", currentVersionCode)
                    val apkUrl = json.optString("download_url", json.optString("apkUrl", "https://raw.githubusercontent.com/Antonionis/TiviPro/main/tivipro.apk"))
                    val changelog = json.optString("changelog", "• Nueva versión disponible.")

                    val isAvailable = isVersionGreater(serverVersionName, currentVersionName) || (serverVersionCode > currentVersionCode)

                    return@withContext UpdateInfo(
                        isUpdateAvailable = isAvailable,
                        versionCode = serverVersionCode,
                        versionName = if (serverVersionName.isNotBlank()) serverVersionName else currentVersionName,
                        apkUrl = if (apkUrl.isNotBlank()) apkUrl else "https://raw.githubusercontent.com/Antonionis/TiviPro/main/tivipro.apk",
                        changelog = changelog
                    )
                }
            }
        } catch (_: Exception) {}

        // Try 2: GitHub Releases API
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API_URL)
                .header("User-Agent", "TiviPro-App")
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body.string()
                    val json = JSONObject(jsonStr)

                    val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
                    val releaseName = json.optString("name", "")
                    val changelog = json.optString("body", "• Nueva versión disponible en GitHub.")

                    var apkUrl = ""
                    val assetsArray = json.optJSONArray("assets")
                    if (assetsArray != null) {
                        for (i in 0 until assetsArray.length()) {
                            val asset = assetsArray.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    if (apkUrl.isBlank()) {
                        apkUrl = "https://raw.githubusercontent.com/Antonionis/TiviPro/main/tivipro.apk"
                    }

                    val serverVersionName = if (tagName.isNotBlank()) tagName else releaseName
                    val isAvailable = isVersionGreater(serverVersionName, currentVersionName)

                    return@withContext UpdateInfo(
                        isUpdateAvailable = isAvailable,
                        versionCode = currentVersionCode + 1,
                        versionName = if (serverVersionName.isNotBlank()) serverVersionName else currentVersionName,
                        apkUrl = apkUrl,
                        changelog = if (changelog.isNotBlank()) changelog else "• Actualización de software desde GitHub Releases."
                    )
                }
            }
        } catch (_: Exception) {}

        UpdateInfo(false, currentVersionCode, currentVersionName, "", "")
    }

    suspend fun fetchCurrentReleaseNotes(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersionName = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.9"
        } catch (_: Exception) {
            "1.9"
        }

        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            9
        }

        val localV19Changelog = "🚀 TiviPro v$currentVersionName - Novedades y Mejoras Principales\n\n" +
                "✨ Rediseño de la Pantalla Principal:\n" +
                "• Nueva interfaz limpia y moderna con portadas en alta definición y accesos directos optimizados a TV, Cine, Series, Deportes y Diferido.\n" +
                "• Botón de Ajustes ubicado de forma accesible en el encabezado superior junto al perfil de usuario.\n\n" +
                "🔍 Búsqueda Independiente en Tiempo Real:\n" +
                "• Buscadores dedicados e independientes dentro de las secciones de Cine, Series y TV en Directo con filtrado ultrarrápido y fluido al teclear.\n\n" +
                "🎬 Experiencia Cinemática en Cine y Series:\n" +
                "• Despliegue automático de portadas, sinopsis de TMDb, logos oficiales transparentes en PNG y tarjetas del reparto técnico.\n" +
                "• Fichas interactivas de los actores con biografía completa en español y sección \"Conocido por\".\n" +
                "• Ficha de detalles con recuadros técnicos informativos: Año de estreno, Géneros y Clasificación por edad oficial.\n" +
                "• Seguimiento inteligente de reproducción: Marcado de episodios vistos (✓ VISTO) y autoselección del siguiente capítulo pendiente.\n\n" +
                "▶ Control Avanzado del Reproductor:\n" +
                "• Avance y retroceso rápido de 10 minutos (botones Skip) y 10 segundos con etiquetas informativas flotantes OSD.\n" +
                "• Aviso emergente automático a los 30 segundos del final para reproducir el siguiente episodio de la serie.\n" +
                "• Barra de progreso táctil y fluida para desplazar el video al minuto exacto deseado.\n\n" +
                "⚽ Alertas Deportivas por Telegram:\n" +
                "• Programación automática de avisos de eventos deportivos a través del Bot de Telegram para recibir alertas en tu teléfono 10 minutos antes del partido, incluso con la TV apagada."

        val client = NetworkModule.getOkHttpClient()

        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API_URL)
                .header("User-Agent", "TiviPro-App")
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body.string()
                    val json = JSONObject(jsonStr)

                    val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
                    val changelog = json.optString("body", "")

                    if (tagName.isNotBlank() && isVersionGreater(currentVersionName, tagName)) {
                        return@withContext UpdateInfo(
                            isUpdateAvailable = false,
                            versionCode = currentVersionCode,
                            versionName = currentVersionName,
                            apkUrl = "",
                            changelog = localV19Changelog
                        )
                    }

                    if (changelog.isNotBlank()) {
                        return@withContext UpdateInfo(
                            isUpdateAvailable = false,
                            versionCode = currentVersionCode,
                            versionName = if (tagName.isNotBlank()) tagName else currentVersionName,
                            apkUrl = "",
                            changelog = changelog
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val fallbackUrl = "https://raw.githubusercontent.com/Antonionis/TiviPro/main/version.json"
            val request = Request.Builder()
                .url(fallbackUrl)
                .header("User-Agent", "TiviPro-App")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body.string()
                    val json = JSONObject(jsonStr)

                    val serverVersionName = json.optString("versionName", currentVersionName)
                    val changelog = json.optString("changelog", "")

                    if (isVersionGreater(currentVersionName, serverVersionName)) {
                        return@withContext UpdateInfo(
                            isUpdateAvailable = false,
                            versionCode = currentVersionCode,
                            versionName = currentVersionName,
                            apkUrl = "",
                            changelog = localV19Changelog
                        )
                    }

                    if (changelog.isNotBlank()) {
                        return@withContext UpdateInfo(
                            isUpdateAvailable = false,
                            versionCode = currentVersionCode,
                            versionName = serverVersionName,
                            apkUrl = "",
                            changelog = changelog
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        UpdateInfo(
            false,
            currentVersionCode,
            currentVersionName,
            "",
            localV19Changelog
        )
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = NetworkModule.getOkHttpClient()

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "TiviPro-App")
                .header("Accept", "*/*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.externalCacheDir
                ?: context.cacheDir

            val apkFile = File(downloadDir, "tivipro_update.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalRead.toFloat() / contentLength.toFloat())
                        }
                    }
                    output.flush()
                }
            }

            installApk(context, apkFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val resInfoList = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
