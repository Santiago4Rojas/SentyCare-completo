package com.example.sentycare

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderHelper {

    fun programarRecordatorio(
        context: Context,
        delayMs: Long,
        paciente: String,
        mensaje: String = "Es hora de realizar la siguiente evaluación al paciente"
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("mensaje", mensaje)
            putExtra("paciente", paciente)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delayMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
