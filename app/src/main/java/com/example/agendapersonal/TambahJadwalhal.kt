package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahJadwalhal : AppCompatActivity() {
    private lateinit var etMusic: EditText
    private val REQUEST_CODE_PICK_AUDIO = 1

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
        etMusic = findViewById(R.id.etMusic)

        // Menampilkan DatePickerDialog saat EditText diklik
        etStartDate.setOnClickListener {
            showDatePickerDialog(etStartDate)
        }

        // Menampilkan TimePickerDialog saat EditText diklik
        etTimeStart.setOnClickListener {
            showTimePickerDialog(etTimeStart)
        }

        etTimeEnd.setOnClickListener {
            showTimePickerDialog(etTimeEnd)
        }

        // Menampilkan file manager untuk memilih musik saat EditText etMusic diklik
        etMusic.setOnClickListener {
            openMusicPicker()
        }
    }

    private fun showDatePickerDialog(etStartDate: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
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
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                editText.setText(formattedTime)
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    private fun openMusicPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*" // Filter hanya untuk file audio
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = getFileName(uri)
                etMusic.setText(fileName) // Menampilkan nama file yang dipilih
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown File"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    fileName = it.getString(index)
                }
            }
        }
        return fileName
    }
}
