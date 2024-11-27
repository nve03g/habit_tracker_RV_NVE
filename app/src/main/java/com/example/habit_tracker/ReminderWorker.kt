package com.example.habit_tracker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

// voeg notificatielogica toe
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class ReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams){
    override fun doWork(): Result {
        val habitName = inputData.getString("habit_name") ?: "Habit"

        // voeg notificatielogica toe
        // maak een intent om app te openen als op notificatie wordt geklikt
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // maak de notificatie
        val notification = NotificationCompat.Builder(applicationContext, "habit_reminders")
            .setSmallIcon(R.drawable.ic_reminder) // bell symbol for reminder notification
            .setContentTitle("Herinnering")
            .setContentText("Tijd om te werken aan: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // sluit notificatie bij klikken
            .build()

        // toon de notificatie
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(habitName.hashCode(), notification)
        }

        return Result.success()
    }
}