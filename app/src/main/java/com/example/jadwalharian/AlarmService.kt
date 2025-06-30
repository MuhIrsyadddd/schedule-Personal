package com.example.jadwalharian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskTitle = intent?.getStringExtra("TASK_TITLE") ?: "Waktunya Tugas!"
        val taskId = intent?.getLongExtra("TASK_ID", 0) ?: 0
        val soundUriString = intent?.getStringExtra("TASK_SOUND_URI")
        val soundUri = soundUriString?.let { Uri.parse(it) }

        // Memulai pemutaran musik
        startMusic(soundUri)

        // Membuat dan menampilkan notifikasi foreground
        showForegroundNotification(taskTitle, taskId, soundUriString)

        return START_NOT_STICKY
    }

    private fun startMusic(soundUri: Uri?) {
        mediaPlayer?.release() // Hentikan musik sebelumnya jika ada

        // ## PERBAIKAN DI SINI ##
        // Gunakan URI yang dipilih pengguna. Jika tidak ada, gunakan alarm default sistem.
        var alarmSoundUri = soundUri
        if (alarmSoundUri == null) {
            alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            // Jika alarm default pun tidak ada, gunakan notifikasi default sebagai fallback terakhir
            if (alarmSoundUri == null) {
                alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }

        try {
            mediaPlayer = MediaPlayer.create(this, alarmSoundUri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showForegroundNotification(taskTitle: String, taskId: Long, soundUriString: String?) {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Berjalan",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Intent untuk aksi "Berhenti"
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "STOP_ACTION"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(this, taskId.toInt() + 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        // Intent untuk aksi "Tidur 5 menit"
        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE_ACTION"
            putExtra("TASK_TITLE", taskTitle)
            putExtra("TASK_ID", taskId)
            putExtra("TASK_SOUND_URI", soundUriString)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(this, taskId.toInt() + 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alarm: $taskTitle")
            .setContentText("Alarm sedang berbunyi.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // Membuat notifikasi tidak bisa di-swipe
            .addAction(0, "Tidur 5 menit", snoozePendingIntent)
            .addAction(0, "Berhenti", stopPendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}