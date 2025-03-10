package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.aprildown.ultimateringtonepicker.RingtonePickerActivity
import xyz.aprildown.ultimateringtonepicker.UltimateRingtonePicker
import java.util.Calendar

class TambahJadwalhal : AppCompatActivity() {

    private lateinit var btnPilihRingtone: Button
    private lateinit var btnSetAlarm: Button
    private lateinit var btnOpenCalendar: Button
    private var selectedRingtoneUri: Uri? = null
    private lateinit var db: AppDatabase
    private lateinit var numberPickerHour: NumberPicker
    private lateinit var numberPickerMinute: NumberPicker
    private lateinit var etJudulJadwal: EditText
    private lateinit var etDeskripsiJadwal: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tambah_jadwalhal)

        db = AppDatabase.getDatabase(this)

        numberPickerHour = findViewById(R.id.numberPickerHour)
        numberPickerMinute = findViewById(R.id.numberPickerMinute)
        btnPilihRingtone = findViewById(R.id.btnPilihRingtone)
        btnSetAlarm = findViewById(R.id.btnSetAlarm)
        btnOpenCalendar = findViewById(R.id.btnOpenCalendar)
        etJudulJadwal = findViewById(R.id.etJudulJadwal)
        etDeskripsiJadwal = findViewById(R.id.etDeskripsiJadwal)

        numberPickerHour.minValue = 0
        numberPickerHour.maxValue = 23

        numberPickerMinute.minValue = 0
        numberPickerMinute.maxValue = 59

        loadSavedAlarm()

        val settings = UltimateRingtonePicker.Settings(
            systemRingtonePicker = UltimateRingtonePicker.SystemRingtonePicker(
                customSection = UltimateRingtonePicker.SystemRingtonePicker.CustomSection(),
                defaultSection = UltimateRingtonePicker.SystemRingtonePicker.DefaultSection(),
                ringtoneTypes = listOf(
                    RingtoneManager.TYPE_RINGTONE,
                    RingtoneManager.TYPE_NOTIFICATION,
                    RingtoneManager.TYPE_ALARM
                )
            ),
            deviceRingtonePicker = UltimateRingtonePicker.DeviceRingtonePicker(
                deviceRingtoneTypes = listOf(
                    UltimateRingtonePicker.RingtoneCategoryType.All,
                    UltimateRingtonePicker.RingtoneCategoryType.Artist,
                    UltimateRingtonePicker.RingtoneCategoryType.Album,
                    UltimateRingtonePicker.RingtoneCategoryType.Folder
                )
            )
        )

        val ringtoneLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val ringtones = RingtonePickerActivity.getPickerResult(result.data!!)
                if (ringtones.isNotEmpty()) {
                    selectedRingtoneUri = ringtones[0].uri
                    btnPilihRingtone.text = "Nada Dering Dipilih"
                }
            }
        }

        btnPilihRingtone.setOnClickListener {
            ringtoneLauncher.launch(
                RingtonePickerActivity.getIntent(
                    context = this,
                    settings = settings,
                    windowTitle = "Pilih Nada Dering"
                )
            )
        }

        btnSetAlarm.setOnClickListener {
            val hour = numberPickerHour.value
            val minute = numberPickerMinute.value
            val selectedDate = btnOpenCalendar.text.toString()
            val judul = etJudulJadwal.text.toString().trim()
            val deskripsi = etDeskripsiJadwal.text.toString().trim()

            if (selectedDate == "Pilih Tanggal") {
                Toast.makeText(this, "Silakan pilih tanggal!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedRingtoneUri == null) {
                Toast.makeText(this, "Silakan pilih nada dering!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (judul.isEmpty()) {
                Toast.makeText(this, "Masukkan judul jadwal!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setAlarm(hour, minute, selectedDate, selectedRingtoneUri!!)
            saveAlarm(hour, minute, selectedRingtoneUri!!.toString(), selectedDate, judul, deskripsi)

            Toast.makeText(this, "Alarm disetel untuk $selectedDate $hour:$minute", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        btnOpenCalendar.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                btnOpenCalendar.text = selectedDate
            }, year, month, day)

            datePickerDialog.show()
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(hour: Int, minute: Int, tanggal: String, ringtoneUri: Uri) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("RINGTONE_URI", ringtoneUri.toString())
        }

        val calendar = Calendar.getInstance()
        val dateParts = tanggal.split("/")
        if (dateParts.size == 3) {
            val day = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val year = dateParts[2].toInt()

            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
        } else {
            Toast.makeText(this, "Format tanggal salah!", Toast.LENGTH_SHORT).show()
            return
        }

        val requestCode = (hour * 60 + minute) // Unik berdasarkan jam dan menit
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (calendar.timeInMillis > System.currentTimeMillis()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }


    private fun saveAlarm(hour: Int, minute: Int, ringtoneUri: String, tanggal: String, judul: String, deskripsi: String) {
        lifecycleScope.launch {
            db.alarmDao().insertAlarm(
                AlarmData(
                    hour = hour,
                    minute = minute,
                    ringtoneUri = ringtoneUri,
                    tanggal = tanggal,
                    judul = judul,
                    deskripsi = deskripsi
                )
            )
        }
    }

    private fun loadSavedAlarm() {
        lifecycleScope.launch {
            val lastAlarm = db.alarmDao().getLatestAlarm()
            if (lastAlarm != null) {
                runOnUiThread {
                    numberPickerHour.value = lastAlarm.hour
                    numberPickerMinute.value = lastAlarm.minute
                    selectedRingtoneUri = Uri.parse(lastAlarm.ringtoneUri)
                    btnPilihRingtone.text = "Nada Dering Dipilih"
                    etJudulJadwal.setText(lastAlarm.judul)
                    etDeskripsiJadwal.setText(lastAlarm.deskripsi)
                }
            }
        }
    }
}
