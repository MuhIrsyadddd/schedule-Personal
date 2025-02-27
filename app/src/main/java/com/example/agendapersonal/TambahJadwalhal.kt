package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahJadwalhal : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tambah_jadwalhal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tambahJadwalRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Menghubungkan EditText
        val etStartDate = findViewById<EditText>(R.id.etStartDate)
        val etTimeStart = findViewById<EditText>(R.id.etTimeStart)
        val etTimeEnd = findViewById<EditText>(R.id.etTimeEnd)

        // Menampilkan DatePickerDialog saat EditText diklik
        etStartDate.setOnClickListener {
            showDatePickerDialog(etStartDate)
        }

        // Menampilkan TimePickerDialog saat EditText waktu diklik
        etTimeStart.setOnClickListener {
            showTimePickerDialog(etTimeStart)
        }

        etTimeEnd.setOnClickListener {
            showTimePickerDialog(etTimeEnd)
        }
    }

    private fun showDatePickerDialog(etStartDate: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Format tanggal menjadi "Senin, 26 Februari 2024"
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                etStartDate.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Format waktu menjadi "HH:mm" (contoh: 10:30)
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                editText.setText(formattedTime)
            },
            hour, minute, true // true = format 24 jam
        )

        timePickerDialog.show()
    }
}
