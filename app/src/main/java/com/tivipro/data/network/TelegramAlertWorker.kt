package com.tivipro.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TelegramAlertWorker(
    private val ctx: Context,
    params: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(ctx, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result = withContext(Dispatchers.IO) {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return@withContext androidx.work.ListenableWorker.Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: ""
        val competition = inputData.getString(KEY_COMPETITION) ?: ""
        val timeDisplay = inputData.getString(KEY_TIME) ?: ""
        val channelName = inputData.getString(KEY_CHANNEL) ?: ""

        val botToken = TelegramAlertManager.getBotToken(ctx)
        val user = TelegramAlertManager.getTelegramUser(ctx)

        if (botToken.isBlank() || user.isBlank()) {
            return@withContext androidx.work.ListenableWorker.Result.failure()
        }

        val alertedIds = TelegramAlertManager.getAlertedEventIds(ctx)
        if (!alertedIds.contains(eventId)) {
            return@withContext androidx.work.ListenableWorker.Result.success()
        }

        val sentIds = TelegramAlertManager.getSentEventIds(ctx)
        if (sentIds.contains(eventId)) {
            return@withContext androidx.work.ListenableWorker.Result.success()
        }

        val message = "⏰ <b>RECORDATORIO DE EVENTO DEPORTIVO</b>\n\n" +
                "🏆 <b>${escapeHtml(title)}</b>\n" +
                "📌 <b>Deporte/Competición</b>: ${escapeHtml(competition)}\n" +
                "🕒 <b>Inicio</b>: ${escapeHtml(timeDisplay)} (en 10 minutos)\n" +
                "📺 <b>Canal</b>: ${escapeHtml(channelName)}"

        val (sent, _) = TelegramAlertManager.sendTelegramMessageDetailed(botToken, user, message)
        if (sent) {
            TelegramAlertManager.markEventAsSent(ctx, eventId)
            androidx.work.ListenableWorker.Result.success()
        } else {
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "title"
        const val KEY_COMPETITION = "competition"
        const val KEY_TIME = "time"
        const val KEY_CHANNEL = "channel"
    }
}
