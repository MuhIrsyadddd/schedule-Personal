package com.example.jadwalharian

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Hentikan layanan (dan musik) terlebih dahulu untuk kedua aksi
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.stopService(serviceIntent)

        val action = intent.action
        if (action == "SNOOZE_ACTION") {
            // Jika aksi "Tidur", jadwalkan ulang alarm 5 menit dari sekarang
            val taskTitle = intent.getStringExtra("TASK_TITLE")
            val taskId = intent.getLongExtra("TASK_ID", 0)
            val soundUri = intent.getStringExtra("TASK_SOUND_URI")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("TASK_TITLE", taskTitle)
                putExtra("TASK_ID", taskId)
                putExtra("TASK_SOUND_URI", soundUri)
            }.let {
                PendingIntent.getBroadcast(context, taskId.toInt(), it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 5)
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                alarmIntent
            )
        }
    }
}