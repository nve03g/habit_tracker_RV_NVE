package com.example.habit_tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.habit_tracker.network.QuotesRepository
import kotlin.coroutines.suspendCoroutine

class QuoteWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        Log.d("QuoteWorker", "doWork started")
        return try {
            val quote = fetchQuoteAsync("gx25opPAZ57nRy/L40ZACw==3ABDh7gOD4447zKe")
            Log.d("QuoteWorker", "Fetched Quote: $quote")
            sendNotification(applicationContext, quote)
            Log.d("QuoteWorker", "Notification sent")
            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("QuoteWorker", "Error in doWork", e)
            androidx.work.ListenableWorker.Result.failure()
        }
    }

    private suspend fun fetchQuoteAsync(apiKey: String): String {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val quotesRepository = QuotesRepository()
            quotesRepository.fetchQuote(apiKey) { quote ->
                continuation.resume(quote) {}
            }
        }
    }

    private fun sendNotification(context: Context, quote: String) {
        Log.d("QuoteWorker", "Sending notification with quote: $quote")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_quote_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dagelijkse Motivatie",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dagelijkse Motivatie")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
        Log.d("QuoteWorker", "Notification displayed")
    }

}