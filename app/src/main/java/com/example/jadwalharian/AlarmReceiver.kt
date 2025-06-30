package com.example.jadwalharian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("TASK_TITLE", intent.getStringExtra("TASK_TITLE"))
            putExtra("TASK_ID", intent.getLongExtra("TASK_ID", 0))
            putExtra("TASK_SOUND_URI", intent.getStringExtra("TASK_SOUND_URI"))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}