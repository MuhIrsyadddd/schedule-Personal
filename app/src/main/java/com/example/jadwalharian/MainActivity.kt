package com.example.jadwalharian

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jadwalharian.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskAdapter: TaskAdapter
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
                askForStoragePermissionAndPickAudio()
            } else {
                Toast.makeText(this, "Izin ditolak.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Database
        db = AppDatabase.getDatabase(this)

        setupRecyclerView()
        observeTasks()
        setupThemeToggleButton()
        askForNotificationPermission()

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList()) { task ->
            deleteTask(task)
        }
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun observeTasks() {
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
            // Batalkan juga alarm yang terkait
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
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    // ... (Fungsi lain seperti applyTheme, pickDateTime, dll tetap sama)

    private fun applyTheme() {
        val sharedPrefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", isSystemInDarkMode())
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun isSystemInDarkMode(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupThemeToggleButton() {
        binding.buttonThemeToggle.setOnClickListener {
            val sharedPrefs = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putBoolean("is_dark_mode", true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putBoolean("is_dark_mode", false)
            }
            editor.apply()
        }
    }

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
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
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

                val newTask = Task(
                    title = taskTitle,
                    timestamp = calendar.timeInMillis,
                    soundUri = soundUri?.toString()
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
                    if(columnIndex >= 0) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                if (result != null) {
                    result = result.substring(cut + 1)
                }
            }
        }
        return result ?: "Unknown"
    }
}
