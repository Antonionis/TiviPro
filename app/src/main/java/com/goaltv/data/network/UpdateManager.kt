package com.goaltv.data.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
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

    private const val VERSION_JSON_URL = "http://130.110.245.130/version.json"
    private const val AUTH_USER = "appuser"
    private const val AUTH_PASS = "r7Gw7PJFq6&op6"

    suspend fun checkForUpdate(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val client = NetworkModule.getOkHttpClient()
            val credential = Credentials.basic(AUTH_USER, AUTH_PASS)

            val request = Request.Builder()
                .url("$VERSION_JSON_URL?t=${System.currentTimeMillis()}")
                .header("Authorization", credential)
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use UpdateInfo(false, 0, "", "", "")
                }

                val jsonStr = response.body.string()
                val json = JSONObject(jsonStr)

                val currentVersionCode = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode
                    }
                } catch (_: Exception) {
                    1
                }

                val currentVersionName = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
                }

                // Support snake_case (version_code) returned by server
                val serverVersionCode = when {
                    json.has("version_code") -> json.optInt("version_code", currentVersionCode)
                    json.has("versionCode") -> json.optInt("versionCode", currentVersionCode)
                    else -> currentVersionCode
                }

                val serverVersionName = when {
                    json.has("version_name") -> json.optString("version_name", "")
                    json.has("versionName") -> json.optString("versionName", "")
                    else -> ""
                }

                val apkUrl = when {
                    json.has("download_url") -> json.optString("download_url", "http://130.110.245.130/goaltv.apk")
                    json.has("apkUrl") -> json.optString("apkUrl", "http://130.110.245.130/goaltv.apk")
                    json.has("apk_url") -> json.optString("apk_url", "http://130.110.245.130/goaltv.apk")
                    else -> "http://130.110.245.130/goaltv.apk"
                }

                val changelog = json.optString("changelog", "• Nueva versión disponible\n• Mejoras de rendimiento y estabilidad")

                val isAvailable = (serverVersionCode > currentVersionCode) ||
                                  (serverVersionName.isNotBlank() && serverVersionName != currentVersionName)

                UpdateInfo(
                    isUpdateAvailable = isAvailable,
                    versionCode = serverVersionCode,
                    versionName = if (serverVersionName.isNotBlank()) serverVersionName else "1.1",
                    apkUrl = apkUrl,
                    changelog = changelog
                )
            }
        } catch (_: Exception) {
            UpdateInfo(false, 0, "", "", "")
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = NetworkModule.getOkHttpClient()
            val credential = Credentials.basic(AUTH_USER, AUTH_PASS)

            val request = Request.Builder()
                .url(apkUrl)
                .header("Authorization", credential)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.externalCacheDir
                ?: context.cacheDir

            val apkFile = File(downloadDir, "goaltv_update.apk")
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