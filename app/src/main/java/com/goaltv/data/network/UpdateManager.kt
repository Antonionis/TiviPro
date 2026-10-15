package com.goaltv.data.network

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

    private const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/GoalSpain/goaltv/releases/latest"

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
        try {
            val client = NetworkModule.getOkHttpClient()

            val request = Request.Builder()
                .url(GITHUB_RELEASES_API_URL)
                .header("User-Agent", "GoalTV-App")
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use UpdateInfo(false, 0, "", "", "")
                }

                val jsonStr = response.body.string()
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
                val releaseName = json.optString("name", "")
                val changelog = json.optString("body", "• Nueva versión disponible en GitHub.")

                val currentVersionName = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "1.0"
                } catch (_: Exception) {
                    "1.0"
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
                    1
                }

                // Extract browser_download_url for goaltv.apk from assets array
                val assetsArray = json.optJSONArray("assets")
                var apkUrl = ""
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
                    apkUrl = "https://github.com/GoalSpain/goaltv/releases/latest/download/goaltv.apk"
                }

                val serverVersionName = if (tagName.isNotBlank()) tagName else releaseName
                val isAvailable = isVersionGreater(serverVersionName, currentVersionName)

                UpdateInfo(
                    isUpdateAvailable = isAvailable,
                    versionCode = currentVersionCode + 1,
                    versionName = if (serverVersionName.isNotBlank()) serverVersionName else "1.3",
                    apkUrl = apkUrl,
                    changelog = if (changelog.isNotBlank()) changelog else "• Actualización de software desde GitHub Releases."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "GoalTV-App")
                .header("Accept", "*/*")
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