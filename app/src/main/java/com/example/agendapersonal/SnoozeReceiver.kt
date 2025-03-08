package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.util.*

class SnoozeReceiver : BroadcastReceiver() {
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")

        Log.d("SnoozeReceiver", "Alarm ditunda 1 menit")

        // Matikan suara alarm
        AlarmReceiver.stopAlarm(context)

        // Hapus notifikasi
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(101)

        // Jadwalkan alarm ulang setelah 1 menit
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("RINGTONE_URI", ringtoneUri)  // Pastikan nada dering tetap sama
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId, newIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 1)  // Tambahkan 1 menit

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}
