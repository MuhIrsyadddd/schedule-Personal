package com.example.agendapersonal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val database = JadwalDatabase.getDatabase(context)
        val jadwalDao = database.jadwalDao()

        GlobalScope.launch(Dispatchers.IO) {
            val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val jadwalList = jadwalDao.getAllJadwal().filter { it.startDate == currentDate && it.timeStart == currentTime }

            if (jadwalList.isNotEmpty()) {
                val jadwal = jadwalList[0] // Ambil jadwal pertama yang cocok
                showNotification(context, jadwal.title, jadwal.description, jadwal.music)
            }
        }
    }

    private fun showNotification(context: Context, title: String, description: String, musicUri: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "agenda_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Agenda Notification", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.notifagenda)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Jika ada musik, mainkan musik
        if (musicUri.isNotEmpty()) {
            val mediaPlayer = MediaPlayer.create(context, Uri.parse(musicUri))
            mediaPlayer?.start()
        }

        notificationManager.notify(1, notificationBuilder.build())
    }
}
