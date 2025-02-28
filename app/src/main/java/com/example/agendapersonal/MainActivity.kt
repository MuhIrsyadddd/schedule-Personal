package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalAdapter
    private lateinit var database: JadwalDatabase

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvGreeting: TextView = findViewById(R.id.tvGreeting)
        val tvDate: TextView = findViewById(R.id.tvDate)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val waktu = when {
            currentHour in 5..11 -> "Pagi"
            currentHour in 12..17 -> "Siang"
            else -> "Malam"
        }

        tvGreeting.text = "$waktu, Agendakan Jadwalmu"

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy\nHH:mm", Locale("id", "ID"))
        val formattedDate = dateFormat.format(calendar.time)
        tvDate.text = formattedDate

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, TambahJadwalhal::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        database = JadwalDatabase.getDatabase(this)

        adapter = JadwalAdapter(emptyList()) { jadwal ->
            deleteJadwal(jadwal)
        }
        recyclerView.adapter = adapter

        loadJadwal()
    }

    override fun onResume() {
        super.onResume()
        loadJadwal() // Memuat ulang data setiap kali aktivitas kembali ke tampilan utama
    }

    private fun loadJadwal() {
        lifecycleScope.launch {
            val jadwalList = database.jadwalDao().getAllJadwal()
            adapter.updateData(jadwalList)

            for (jadwal in jadwalList) {
                scheduleNotification(this@MainActivity, jadwal)
            }

            val tvDescription: TextView = findViewById(R.id.tvDescription)
            tvDescription.visibility = if (jadwalList.isNotEmpty()) View.GONE else View.VISIBLE
        }
    }


    private fun deleteJadwal(jadwal: Jadwal) {
        lifecycleScope.launch {
            database.jadwalDao().deleteJadwal(jadwal.id)
            loadJadwal()
        }
    }

    private fun scheduleNotification(context: Context, jadwal: Jadwal) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", jadwal.title)
            putExtra("description", jadwal.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, jadwal.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dateTimeString = "${jadwal.startDate} ${jadwal.timeStart}"
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale("id", "ID")) // Sesuaikan dengan format yang digunakan di database
        val date = try {
            dateFormat.parse(dateTimeString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        date?.let {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.time, pendingIntent)
        }
    }
}
