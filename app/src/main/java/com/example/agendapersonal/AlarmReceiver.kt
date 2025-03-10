package com.example.agendapersonal

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private var mediaPlayer: MediaPlayer? = null

        fun stopAlarm(context: Context) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI")
        val alarmId = intent.getIntExtra("ALARM_ID", 0)

        val currentDate = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
        val db = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            val lastAlarm = db.alarmDao().getLatestAlarm()
            val alarmDate = lastAlarm?.tanggal

            Log.d("AlarmReceiver", "Tanggal alarm dari database: $alarmDate")
            Log.d("AlarmReceiver", "Tanggal saat ini: $currentDate")

            if (alarmDate == currentDate && !ringtoneUri.isNullOrEmpty()) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, Uri.parse(ringtoneUri))
                        prepare()
                        start()
                        setOnCompletionListener {
                            stopAlarm(context)
                        }
                    }

                    showNotification(context, alarmId, ringtoneUri)

                    Log.d("AlarmReceiver", "Alarm berbunyi!")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Gagal memutar nada dering: ${e.message}")
                }
            } else {
                Log.d("AlarmReceiver", "Alarm tidak berbunyi karena tanggal tidak cocok")
            }
        }
    }

    private fun showNotification(context: Context, alarmId: Int, ringtoneUri: String?) {
        val channelId = "alarm_channel"
        val notificationId = 101

        // Cek izin sebelum mengirimkan notifikasi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("AlarmReceiver", "Izin notifikasi tidak diberikan")
                return
            }
        }

        // Buat Notification Channel jika Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Ambil judul alarm dari database
        val db = AppDatabase.getDatabase(context)
        var judulAlarm = "Alarm Berbunyi!"  // Default jika tidak ditemukan

        CoroutineScope(Dispatchers.IO).launch {
            val alarmData = db.alarmDao().getLatestAlarm()
            if (alarmData != null) {
                judulAlarm = alarmData.judul  // Gunakan judul dari database
            }

            // Intent untuk "Abaikan" (Menghentikan alarm)
            val dismissIntent = Intent(context, DismissReceiver::class.java).apply {
                putExtra("ALARM_ID", alarmId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent untuk "Tidur Sebentar" (Menunda alarm 1 menit)
            val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
                putExtra("ALARM_ID", alarmId)
                putExtra("RINGTONE_URI", ringtoneUri)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notifagenda)
                .setContentTitle(judulAlarm)  // Tampilkan judul alarm di sini
                .setContentText("Klik Abaikan untuk menghentikan, Tidur Sebentar untuk tunda 1 menit.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.notifagenda, "Abaikan", dismissPendingIntent)
                .addAction(R.drawable.notifagenda, "Tidur Sebentar", snoozePendingIntent)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        }
    }

}
