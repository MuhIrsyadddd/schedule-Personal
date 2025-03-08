package com.example.agendapersonal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DismissReceiver", "Alarm dihentikan oleh pengguna")

        // Hentikan suara alarm
        AlarmReceiver.stopAlarm(context)

        // Hapus notifikasi
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(101)
    }
}
