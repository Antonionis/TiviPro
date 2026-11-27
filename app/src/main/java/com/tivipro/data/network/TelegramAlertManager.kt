package com.tivipro.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tivipro.model.SportsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramAlertManager {

    private const val PREFS_NAME = "tivipro_telegram_prefs"
    const val DEFAULT_BOT_USERNAME = "@tivipro_bot"
    const val DEFAULT_BOT_TOKEN = "9122014698:AAF-x9_80tPWW3DoprfjZu0-OnBssW2cdjE"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTelegramUser(context: Context): String {
        return getPrefs(context).getString("telegram_user", "") ?: ""
    }

    fun getBotToken(context: Context): String {
        val saved = getPrefs(context).getString("bot_token", "") ?: ""
        return saved.ifBlank { DEFAULT_BOT_TOKEN }
    }

    fun saveTelegramConfig(context: Context, user: String, customToken: String = "") {
        val formattedUser = user.trim().let { u ->
            if (u.isBlank()) ""
            else if (u.startsWith("@") || u.all { it.isDigit() || it == '-' }) u
            else "@$u"
        }
        getPrefs(context).edit()
            .putString("telegram_user", formattedUser)
            .putString("bot_token", customToken.trim())
            .apply()

        if (formattedUser.isNotBlank()) {
            @Suppress("OPT_IN_USAGE")
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    DeviceManager.syncTelegramUserToDatabase(context, formattedUser)
                } catch (_: Exception) {}
            }
        }
    }

    fun clearTelegramConfig(context: Context) {
        getPrefs(context).edit()
            .remove("telegram_user")
            .remove("alerted_event_ids")
            .apply()

        @Suppress("OPT_IN_USAGE")
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                DeviceManager.syncTelegramUserToDatabase(context, "")
            } catch (_: Exception) {}
        }
    }

    fun getAlertedEventIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet("alerted_event_ids", emptySet()) ?: emptySet()
    }

    fun isEventAlerted(context: Context, eventId: String): Boolean {
        return getAlertedEventIds(context).contains(eventId)
    }

    fun scheduleWorkerForEvent(context: Context, event: SportsEvent) {
        try {
            val rawTimestamp = event.startTimestamp
            if (rawTimestamp <= 0L) return
            val eventStartSec = if (rawTimestamp > 10_000_000_000L) rawTimestamp / 1000L else rawTimestamp
            val nowSec = System.currentTimeMillis() / 1000L

            if (nowSec > eventStartSec + 7200L) return

            val alertTimeSec = eventStartSec - 600L
            val delaySec = alertTimeSec - nowSec
            val initialDelaySec = if (delaySec > 0L) delaySec else 0L

            val workManager = androidx.work.WorkManager.getInstance(context)

            val inputData = androidx.work.Data.Builder()
                .putString(TelegramAlertWorker.KEY_EVENT_ID, event.id)
                .putString(TelegramAlertWorker.KEY_TITLE, event.title)
                .putString(TelegramAlertWorker.KEY_COMPETITION, event.competition.ifBlank { event.sportCategory })
                .putString(TelegramAlertWorker.KEY_TIME, event.dateTimeDisplay)
                .putString(TelegramAlertWorker.KEY_CHANNEL, event.channelName)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<TelegramAlertWorker>()
                .setInitialDelay(initialDelaySec, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("telegram_alert_${event.id}")
                .build()

            workManager.enqueueUniqueWork(
                "work_telegram_${event.id}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelWorkerForEvent(context: Context, eventId: String) {
        try {
            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.cancelUniqueWork("work_telegram_$eventId")
        } catch (_: Exception) {}
    }

    fun toggleEventAlert(context: Context, event: SportsEvent): Boolean {
        val current = getAlertedEventIds(context).toMutableSet()
        val isNowAlerted = if (current.contains(event.id)) {
            current.remove(event.id)
            cancelWorkerForEvent(context, event.id)
            false
        } else {
            current.add(event.id)
            scheduleWorkerForEvent(context, event)
            true
        }
        getPrefs(context).edit().putStringSet("alerted_event_ids", current).apply()
        return isNowAlerted
    }

    fun toggleEventAlert(context: Context, eventId: String): Boolean {
        val current = getAlertedEventIds(context).toMutableSet()
        val isNowAlerted = if (current.contains(eventId)) {
            current.remove(eventId)
            cancelWorkerForEvent(context, eventId)
            false
        } else {
            current.add(eventId)
            true
        }
        getPrefs(context).edit().putStringSet("alerted_event_ids", current).apply()
        return isNowAlerted
    }

    fun isConfigured(context: Context): Boolean {
        return getTelegramUser(context).isNotBlank()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    suspend fun sendTelegramMessageDetailed(botToken: String, chatId: String, text: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) return@withContext Pair(false, "Falta el Chat ID o Token de Telegram")
        try {
            val url = "https://api.telegram.org/bot${botToken.trim()}/sendMessage"
            val jsonBody = JSONObject().apply {
                put("chat_id", chatId.trim())
                put("text", text)
                put("parse_mode", "HTML")
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val client = NetworkModule.getOkHttpClient()
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Pair(true, "Mensaje enviado con éxito")
                } else {
                    val desc = try {
                        JSONObject(responseStr).optString("description", "Error HTTP ${response.code}")
                    } catch (_: Exception) {
                        "Error HTTP ${response.code}"
                    }
                    val formattedMsg = if (desc.contains("chat not found", ignoreCase = true)) {
                        "Telegram requiere tu Chat ID numérico (ej: 987654321). Consulta en Telegram a @userinfobot"
                    } else if (desc.contains("blocked", ignoreCase = true)) {
                        "Abre @tivipro_bot en Telegram y pulsa INICIAR / /start primero"
                    } else {
                        desc
                    }
                    Pair(false, formattedMsg)
                }
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Error de red al conectar con Telegram")
        }
    }

    suspend fun sendTelegramMessage(botToken: String, chatId: String, text: String): Boolean {
        return sendTelegramMessageDetailed(botToken, chatId, text).first
    }

    suspend fun sendTestMessageDetailed(context: Context, user: String, customToken: String = ""): Pair<Boolean, String> {
        val token = customToken.ifBlank { getBotToken(context) }
        val targetUser = user.trim().let { u ->
            if (u.isBlank()) ""
            else if (u.startsWith("@") || u.all { it.isDigit() || it == '-' }) u
            else "@$u"
        }

        val msg = "⚽ <b>TIVIPRO - ALERTAS DE TELEGRAM</b>\n\n" +
                "✅ ¡Conexión con el bot ${escapeHtml(DEFAULT_BOT_USERNAME)} configurada para <b>${escapeHtml(targetUser)}</b>!\n\n" +
                "Recibirás avisos automáticos en tu Telegram 10 minutos antes de que comiencen tus partidos y eventos deportivos marcados."

        val result = sendTelegramMessageDetailed(token, targetUser, msg)
        if (result.first) {
            saveTelegramConfig(context, targetUser, token)
        }
        return result
    }

    suspend fun sendTestMessage(context: Context, user: String, customToken: String = ""): Boolean {
        return sendTestMessageDetailed(context, user, customToken).first
    }

    fun getSentEventIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet("sent_event_ids", emptySet()) ?: emptySet()
    }

    fun markEventAsSent(context: Context, eventId: String) {
        val current = getSentEventIds(context).toMutableSet()
        current.add(eventId)
        getPrefs(context).edit().putStringSet("sent_event_ids", current).apply()
    }

    private fun cleanupExpiredAlerts(context: Context, events: List<SportsEvent>) {
        val currentAlerts = getAlertedEventIds(context).toMutableSet()
        if (currentAlerts.isEmpty()) return
        val nowSec = System.currentTimeMillis() / 1000L

        var modified = false
        for (event in events) {
            if (!currentAlerts.contains(event.id)) continue
            val rawTimestamp = event.startTimestamp
            if (rawTimestamp <= 0L) continue
            val eventStartSec = if (rawTimestamp > 10_000_000_000L) rawTimestamp / 1000L else rawTimestamp

            // Only remove if 2 hours (7200 sec) have passed after match start time
            if (nowSec > (eventStartSec + 7200L)) {
                currentAlerts.remove(event.id)
                modified = true
            }
        }
        if (modified) {
            getPrefs(context).edit().putStringSet("alerted_event_ids", currentAlerts).apply()
        }
    }

    suspend fun checkAndTriggerPendingAlerts(context: Context, events: List<SportsEvent>) = withContext(Dispatchers.IO) {
        val user = getTelegramUser(context)
        val botToken = getBotToken(context)
        if (user.isBlank() || botToken.isBlank()) return@withContext

        // Schedule WorkManager tasks for all alerted events so they fire even if app is closed!
        val alertedIds = getAlertedEventIds(context)
        if (alertedIds.isEmpty()) return@withContext

        for (event in events) {
            if (alertedIds.contains(event.id)) {
                scheduleWorkerForEvent(context, event)
            }
        }

        // Clean up events that are finished (2 hours after start)
        cleanupExpiredAlerts(context, events)

        val sentIds = getSentEventIds(context)
        val nowSec = System.currentTimeMillis() / 1000L

        for (event in events) {
            if (!alertedIds.contains(event.id)) continue
            if (sentIds.contains(event.id)) continue

            val rawTimestamp = event.startTimestamp
            if (rawTimestamp <= 0L) continue

            val eventStartSec = if (rawTimestamp > 10_000_000_000L) rawTimestamp / 1000L else rawTimestamp
            val secondsUntilStart = eventStartSec - nowSec

            // Trigger alert if event starts in <= 15 minutes (or started in the last 5 minutes)
            if (secondsUntilStart in -300..900) {
                val message = "⏰ <b>RECORDATORIO DE EVENTO DEPORTIVO</b>\n\n" +
                        "🏆 <b>${escapeHtml(event.title)}</b>\n" +
                        "📌 <b>Deporte/Competición</b>: ${escapeHtml(event.competition.ifBlank { event.sportCategory })}\n" +
                        "🕒 <b>Inicio</b>: ${escapeHtml(event.dateTimeDisplay)} (en 10 minutos)\n" +
                        "📺 <b>Canal</b>: ${escapeHtml(event.channelName)}"

                val (sent, _) = sendTelegramMessageDetailed(botToken, user, message)
                if (sent) {
                    markEventAsSent(context, event.id)
                }
            }
        }
    }
}
