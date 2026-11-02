package com.example.mitrivia

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar
import java.util.TimeZone

class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val p = context.getSharedPreferences("trivia", Context.MODE_PRIVATE)
            if (!p.getBoolean("recordatorio", false)) return
            val ahora = System.currentTimeMillis()
            val hoy = ((ahora + TimeZone.getDefault().getOffset(ahora)) / 86400000L).toInt()
            val ultimo = p.getInt("dia_ultimo", -1)
            if (ultimo == hoy) return
            val racha = if (ultimo == hoy - 1) p.getInt("dia_racha", 0) else 0

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel("recordatorio", "Recordatorio diario", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            val abrir = Intent(context, MainActivity::class.java)
            abrir.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val pi = PendingIntent.getActivity(context, 1, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val texto = if (racha > 0) {
                "Llevás $racha días seguidos. ¡No cortes la racha! 🔥"
            } else {
                "Tu pregunta del día te espera 🔥"
            }
            val n = NotificationCompat.Builder(context, "recordatorio")
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle("Trivia Max")
                .setContentText(texto)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(1, n)
        } catch (e: Exception) {
            // sin permiso o sin canal: no pasa nada
        }
    }
}

class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = context.getSharedPreferences("trivia", Context.MODE_PRIVATE)
        if (p.getBoolean("recordatorio", false)) {
            Recordatorios.programar(context)
        }
    }
}

object Recordatorios {
    private fun pendiente(context: Context): PendingIntent {
        val i = Intent(context, RecordatorioReceiver::class.java)
        return PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun programar(context: Context) {
        try {
            val p = context.getSharedPreferences("trivia", Context.MODE_PRIVATE)
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, p.getInt("recordatorio_hora", 20))
            c.set(Calendar.MINUTE, p.getInt("recordatorio_min", 0))
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (c.timeInMillis <= System.currentTimeMillis()) {
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, AlarmManager.INTERVAL_DAY, pendiente(context))
        } catch (e: Exception) {
            // nada
        }
    }

    fun cancelar(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendiente(context))
        } catch (e: Exception) {
            // nada
        }
    }
}
