package com.example.sentycare

import android.graphics.BitmapFactory
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mensaje = intent.getStringExtra("mensaje")
            ?: "Es hora de realizar la siguiente evaluación al paciente"
        val paciente = intent.getStringExtra("paciente") ?: ""

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de evaluación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alertas de reevaluación UCI pediátrica" }
            nm.createNotificationChannel(channel)
        }

        // Tap opens the app, preserving the existing back stack (session included)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(context, 0, openIntent, pendingFlags)

        val titulo = if (paciente.isNotBlank()) "Recordatorio — $paciente" else "Recordatorio UCI"
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.logosentycare)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "recordatorio_evaluacion"
    }
}

