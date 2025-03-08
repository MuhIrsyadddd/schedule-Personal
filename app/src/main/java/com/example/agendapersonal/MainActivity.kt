package com.example.agendapersonal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var database: AppDatabase
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Minta izin notifikasi jika belum diberikan (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi database
        database = AppDatabase.getDatabase(this)

        // Ambil data dari database dan tampilkan
        loadAlarms()

        // Floating Action Button untuk menambah alarm baru
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            val intent = Intent(this, TambahJadwalhal::class.java)
            startActivity(intent)
        }

        // Inisialisasi TextView
        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)

        // Mulai update waktu secara real-time
        startClock()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Handle jika pengguna menolak izin
        }
    }

    private fun startClock() {
        val runnable = object : Runnable {
            override fun run() {
                updateDateTime()
                handler.postDelayed(this, 1000) // Perbarui setiap 1 detik
            }
        }
        handler.post(runnable)
    }

    private fun updateDateTime() {
        val locale = Locale("id", "ID")
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", locale) // Senin, 17 Juli 2000
        val timeFormat = SimpleDateFormat("HH:mm:ss", locale) // 10:10:10

        val currentDate = Date()
        tvGreeting.text = dateFormat.format(currentDate)
        tvDate.text = timeFormat.format(currentDate)
    }

    private fun loadAlarms() {
        lifecycleScope.launch {
            val alarmList = database.alarmDao().getAllAlarms().toMutableList()
            alarmAdapter = AlarmAdapter(alarmList) { alarm ->
                deleteAlarm(alarm)
            }
            recyclerView.adapter = alarmAdapter
        }
    }

    private fun deleteAlarm(alarm: AlarmData) {
        lifecycleScope.launch {
            database.alarmDao().deleteAlarmById(alarm.id)
            alarmAdapter.removeItem(alarm)
        }
    }
}
