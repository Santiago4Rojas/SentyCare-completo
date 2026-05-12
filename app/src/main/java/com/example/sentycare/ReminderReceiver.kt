package com.example.sentycare

import android.app.NotificationChannel
import android.app.NotificationManager
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

        val titulo = if (paciente.isNotBlank()) "Recordatorio — $paciente" else "Recordatorio UCI"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "recordatorio_evaluacion"
    }
}
