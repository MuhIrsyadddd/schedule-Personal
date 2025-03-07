package com.example.agendapersonal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
        val currentDate = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date()) // Sesuaikan format dengan database
        val db = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            val lastAlarm = db.alarmDao().getLatestAlarm()
            val alarmDate = lastAlarm?.tanggal

            Log.d("AlarmReceiver", "Tanggal alarm dari database: $alarmDate")
            Log.d("AlarmReceiver", "Tanggal saat ini: $currentDate")

            if (alarmDate == currentDate && !ringtoneUri.isNullOrEmpty()) {
                try {
                    val mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, Uri.parse(ringtoneUri))
                        prepare()
                        start()
                    }
                    Log.d("AlarmReceiver", "Alarm berbunyi!")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Gagal memutar nada dering: ${e.message}")
                }
            } else {
                Log.d("AlarmReceiver", "Alarm tidak berbunyi karena tanggal tidak cocok")
            }
        }
    }
}
