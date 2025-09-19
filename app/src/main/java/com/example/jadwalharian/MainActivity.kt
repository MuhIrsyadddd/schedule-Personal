package com.example.jadwalharian

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jadwalharian.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var dateAdapter: DateAdapter
    private lateinit var db: AppDatabase
    private var selectedSoundUri: Uri? = null

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedSoundUri = it
            Toast.makeText(this, "Lagu dipilih: ${getFileName(it)}", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Izin diberikan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin ditolak.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupUI()
        setupRecyclerViews()
        observeTasks()
        askForNotificationPermission()
    }

    private fun setupUI() {
        updateDateDisplay()
        // Error fabAddTask diperbaiki: Menggunakan ID tombol header yang baru
        binding.buttonAddTaskHeader.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupRecyclerViews() {
        // Setup Task RecyclerView
        // Menghapus onDeleteClick karena tombol hapus tidak ada di UI baru
        taskAdapter = TaskAdapter(emptyList())
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        // Setup Date RecyclerView
        val dateList = generateDateList()
        dateAdapter = DateAdapter(dateList)
        binding.recyclerViewDates.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
            // Scroll ke tanggal hari ini
            val todayIndex = dateList.indexOfFirst { it.isSelected }
            if (todayIndex != -1) {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(todayIndex - 2, 0) // Center today's date
            }
        }
    }

    private fun generateDateList(): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -15) // Mulai dari 15 hari yang lalu
        for (i in 0..29) { // Buat list untuk 30 hari
            val isToday = i == 15
            dates.add(DateItem(calendar.time, isToday)) // Tandai hari ini sebagai terpilih
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    private fun updateDateDisplay() {
        val calendar = Calendar.getInstance()
        val monthYearFormat = SimpleDateFormat("MMMM, yyyy", Locale.getDefault())
        binding.textViewMonthYear.text = monthYearFormat.format(calendar.time)
    }

    private fun observeTasks() {
        // TODO: Nanti, filter tugas berdasarkan tanggal yang dipilih dari dateAdapter
        db.taskDao().getAllTasks().observe(this) { tasks ->
            tasks?.let {
                taskAdapter.updateTasks(it)
                checkEmptyView(it)
            }
        }
    }

    private fun deleteTask(task: Task) {
        lifecycleScope.launch {
            db.taskDao().deleteTask(task)
            cancelAlarm(task)
            Toast.makeText(this@MainActivity, "Jadwal '${task.title}' dihapus.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkEmptyView(tasks: List<Task>) {
        binding.textViewEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun addTask(task: Task) {
        lifecycleScope.launch {
            db.taskDao().insertTask(task)
            scheduleAlarm(task)
        }
    }

    private fun scheduleAlarm(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_ID", task.id)
            putExtra("TASK_SOUND_URI", task.soundUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, task.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.timestamp, pendingIntent)
        Toast.makeText(this, "Alarm untuk '${task.title}' telah disetel.", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, task.id.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    // Fungsi applyTheme dan setupThemeToggleButton dihapus karena tombolnya sudah tidak ada.

    private fun showAddTaskDialog() {
        selectedSoundUri = null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val editTextTaskTitle = dialogView.findViewById<EditText>(R.id.editTextTaskTitle)
        val buttonSelectSound = dialogView.findViewById<Button>(R.id.buttonSelectSound)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Tambah Jadwal Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()
        buttonSelectSound.setOnClickListener {
            askForStoragePermissionAndPickAudio()
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val taskTitle = editTextTaskTitle.text.toString()
                if (taskTitle.isBlank()) {
                    Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                } else {
                    pickDateTime(taskTitle, selectedSoundUri)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun pickDateTime(taskTitle: String, soundUri: Uri?) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Waktu yang dipilih sudah lewat", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                // Error status & duration diperbaiki: Memberikan nilai default
                val newTask = Task(
                    title = taskTitle,
                    timestamp = calendar.timeInMillis,
                    soundUri = soundUri?.toString(),
                    status = "Upcoming",
                    duration = "1.5 Hours" // Anda bisa menambahkan input untuk durasi di dialog
                )
                addTask(newTask)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun askForStoragePermissionAndPickAudio() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                selectAudioLauncher.launch(arrayOf("audio/*"))
            }
            shouldShowRequestPermissionRationale(permission) -> {
                AlertDialog.Builder(this)
                    .setTitle("Izin Penyimpanan Diperlukan")
                    .setMessage("Aplikasi ini memerlukan izin untuk mengakses file audio agar dapat dijadikan nada alarm.")
                    .setPositiveButton("OK") { _, _ -> requestPermissionLauncher.launch(permission) }
                    .create()
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            // PERBAIKAN: Menambahkan null-check sebelum memanggil substring
            result?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) {
                    result = path.substring(cut + 1)
                }
            }
        }
        return result ?: "Unknown"
    }
}

