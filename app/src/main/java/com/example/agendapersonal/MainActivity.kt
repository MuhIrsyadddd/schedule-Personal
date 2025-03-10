package com.example.agendapersonal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        database = AppDatabase.getDatabase(this)

        loadAlarms()

        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            val intent = Intent(this, TambahJadwalhal::class.java)
            startActivity(intent)
        }

        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)
        startClock()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) { }
    }

    private fun startClock() {
        val runnable = object : Runnable {
            override fun run() {
                updateDateTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun updateDateTime() {
        val locale = Locale("id", "ID")
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", locale)
        val timeFormat = SimpleDateFormat("HH:mm:ss", locale)

        val currentDate = Date()
        tvGreeting.text = dateFormat.format(currentDate)
        tvDate.text = timeFormat.format(currentDate)
    }

    private fun loadAlarms() {
        lifecycleScope.launch {
            val alarmList = database.alarmDao().getAllAlarms().toMutableList()

            // Sorting by date (dd/MM/yyyy) and then by time (hour, minute)
            alarmList.sortWith(compareBy(
                { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.tanggal) },
                { it.hour },
                { it.minute }
            ))

            alarmAdapter = AlarmAdapter(alarmList) { alarm ->
                deleteAlarm(alarm)
            }
            recyclerView.adapter = alarmAdapter

            updateTvDescriptionVisibility(alarmList)
        }
    }


    private fun deleteAlarm(alarm: AlarmData) {
        lifecycleScope.launch {
            database.alarmDao().deleteAlarmById(alarm.id)
            alarmAdapter.removeItem(alarm)
            updateTvDescriptionVisibility(alarmAdapter.getAlarmList())
        }
    }

    private fun updateTvDescriptionVisibility(alarmList: List<AlarmData>) {
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        if (alarmList.isEmpty()) {
            tvDescription.visibility = TextView.VISIBLE
        } else {
            tvDescription.visibility = TextView.GONE
        }
    }

    private fun checkAndSetAlarms() {
        lifecycleScope.launch {
            val alarms = database.alarmDao().getAllAlarms()
            val currentTime = Calendar.getInstance()

            alarms.forEach { alarm ->
                val alarmCalendar = Calendar.getInstance().apply {
                    val dateParts = alarm.tanggal.split("/")
                    if (dateParts.size == 3) {
                        set(Calendar.YEAR, dateParts[2].toInt())
                        set(Calendar.MONTH, dateParts[1].toInt() - 1)
                        set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                    }
                }

                if (alarmCalendar.after(currentTime)) {
                    setAlarm(alarm.hour, alarm.minute, alarm.tanggal, Uri.parse(alarm.ringtoneUri))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndSetAlarms()
    }

    private fun setAlarm(hour: Int, minute: Int, tanggal: String, ringtoneUri: Uri) {
        // Implementasikan fungsi untuk menyalakan alarm di sini
    }
}
