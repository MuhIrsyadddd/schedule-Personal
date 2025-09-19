package com.example.jadwalharian

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.DialogInterface
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private var soundSelectionTextView: android.widget.TextView? = null // <-- TAMBAHKAN BARIS INI

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedSoundUri = it

            // PERBARUI UI SECARA REAL-TIME
            // Tanda tanya (?) memastikan aplikasi tidak crash jika referensinya null
            soundSelectionTextView?.text = "Nada: ${getFileName(it)}"

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
        setupRecyclerViews() // Adapter sekarang sudah punya listener

        // GANTI `observeTasks()` dengan ini untuk memuat tugas hari ini saat pertama kali
        val today = generateDateList().find { it.isSelected }?.date ?: Date()
        loadTasksForDate(today)

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
        // PERBARUI BAGIAN INI: Tambahkan lambda untuk onStatusUpdate
        taskAdapter = TaskAdapter(emptyList()) { taskToUpdate, newStatus ->
            // Buat salinan objek tugas dengan status baru
            val updatedTask = taskToUpdate.copy(status = newStatus)
            // Panggil fungsi untuk menyimpan perubahan ke database
            updateTask(updatedTask)
        }

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        val dateList = generateDateList()
        dateAdapter = DateAdapter(dateList) { selectedDate ->
            loadTasksForDate(selectedDate)
        }

        binding.recyclerViewDates.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
            val todayIndex = dateList.indexOfFirst { it.isSelected }
            if (todayIndex != -1) {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(todayIndex - 2, 0)
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

    // HAPUS fungsi observeTasks() yang lama, dan GANTI dengan ini
    private fun loadTasksForDate(selectedDate: Date) {
        // Atur kalender ke awal hari (00:00:00)
        val startOfDay = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Atur kalender ke akhir hari (23:59:59)
        val endOfDay = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Panggil fungsi DAO yang baru dan observe hasilnya
        db.taskDao().getTasksByDate(startOfDay.timeInMillis, endOfDay.timeInMillis).observe(this) { tasks ->
            tasks?.let {
                taskAdapter.updateTasks(it)
                checkEmptyView(it)
            }
        }
    }

    private fun updateTask(task: Task) {
        lifecycleScope.launch {
            db.taskDao().updateTask(task)
            // Toast opsional untuk memberi tahu user
            Toast.makeText(this@MainActivity, "Status '${task.title}' diperbarui", Toast.LENGTH_SHORT).show()
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

    // Pastikan impor di atas sudah diubah ke com.google.android.material.dialog.MaterialAlertDialogBuilder

    private fun showAddTaskDialog() {
        selectedSoundUri = null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)

        val textViewSelectDate = dialogView.findViewById<android.widget.TextView>(R.id.textViewSelectDate)
        val editTextTaskTitle = dialogView.findViewById<EditText>(R.id.editTextTaskTitle)
        val textViewStartTime = dialogView.findViewById<android.widget.TextView>(R.id.textViewStartTime)
        val textViewEndTime = dialogView.findViewById<android.widget.TextView>(R.id.textViewEndTime)
        val buttonSelectSound = dialogView.findViewById<Button>(R.id.buttonSelectSound)

        // 1. Hubungkan TextView dari dialog ke variabel class
        soundSelectionTextView = dialogView.findViewById(R.id.textViewSoundSelected)
        // Atur teks awal, jaga-jaga jika pengguna tidak memilih lagu baru
        soundSelectionTextView?.text = "Nada: Default"

        val taskDateCalendar = Calendar.getInstance()
        val startCalendar = Calendar.getInstance()
        val endCalendar = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        textViewSelectDate.text = dateFormat.format(taskDateCalendar.time)

        textViewSelectDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                taskDateCalendar.set(Calendar.YEAR, year)
                taskDateCalendar.set(Calendar.MONTH, month)
                taskDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                textViewSelectDate.text = dateFormat.format(taskDateCalendar.time)
            }, taskDateCalendar.get(Calendar.YEAR), taskDateCalendar.get(Calendar.MONTH), taskDateCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        textViewStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                startCalendar.set(Calendar.MINUTE, minute)
                textViewStartTime.text = timeFormat.format(startCalendar.time)
            }, startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE), true).show()
        }

        textViewEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                endCalendar.set(Calendar.MINUTE, minute)
                textViewEndTime.text = timeFormat.format(endCalendar.time)
            }, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE), true).show()
        }

        buttonSelectSound.setOnClickListener {
            askForStoragePermissionAndPickAudio()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        // 2. Penting: Bersihkan referensi saat dialog ditutup
        dialog.setOnDismissListener {
            soundSelectionTextView = null
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val taskTitle = editTextTaskTitle.text.toString()

                if (taskTitle.isBlank()) {
                    Toast.makeText(this, "Task Name tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (textViewStartTime.text.contains("Pilih") || textViewEndTime.text.contains("Pilih")) {
                    Toast.makeText(this, "Silakan pilih waktu mulai dan selesai", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                startCalendar.set(
                    taskDateCalendar.get(Calendar.YEAR),
                    taskDateCalendar.get(Calendar.MONTH),
                    taskDateCalendar.get(Calendar.DAY_OF_MONTH)
                )
                endCalendar.set(
                    taskDateCalendar.get(Calendar.YEAR),
                    taskDateCalendar.get(Calendar.MONTH),
                    taskDateCalendar.get(Calendar.DAY_OF_MONTH)
                )

                if (endCalendar.timeInMillis <= startCalendar.timeInMillis) {
                    Toast.makeText(this, "Waktu selesai harus setelah waktu mulai", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val durationString = calculateDurationString(startCalendar, endCalendar)

                val newTask = Task(
                    title = taskTitle,
                    timestamp = startCalendar.timeInMillis,
                    soundUri = selectedSoundUri?.toString(),
                    status = "Upcoming",
                    duration = durationString
                )

                addTask(newTask)
                dialog.dismiss() // Ini akan memicu OnDismissListener
            }
        }
        dialog.show()
    }
    private fun calculateDurationString(start: Calendar, end: Calendar): String {
        val diff = end.timeInMillis - start.timeInMillis
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 && remainingMinutes > 0 -> "$hours Hours $remainingMinutes Minutes"
            hours > 0 -> "$hours Hours"
            else -> "$minutes Minutes"
        }
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
                // PERBAIKAN DI SINI
                MaterialAlertDialogBuilder(this)
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

