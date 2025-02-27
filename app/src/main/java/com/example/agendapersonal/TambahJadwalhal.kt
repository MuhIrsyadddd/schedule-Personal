package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TambahJadwalhal : AppCompatActivity() {
    private lateinit var database: JadwalDatabase
    private lateinit var etMusic: EditText
    private val REQUEST_CODE_PICK_AUDIO = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tambah_jadwalhal)

        database = JadwalDatabase.getDatabase(this)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etStartDate = findViewById<EditText>(R.id.etStartDate)
        val etTimeStart = findViewById<EditText>(R.id.etTimeStart)
        val etTimeEnd = findViewById<EditText>(R.id.etTimeEnd)
        etMusic = findViewById(R.id.etMusic)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnDone = findViewById<Button>(R.id.btnDone)

        etStartDate.setOnClickListener { showDatePickerDialog(etStartDate) }
        etTimeStart.setOnClickListener { showTimePickerDialog(etTimeStart) }
        etTimeEnd.setOnClickListener { showTimePickerDialog(etTimeEnd) }
        etMusic.setOnClickListener { openMusicPicker() }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString()
            val startDate = etStartDate.text.toString()
            val timeStart = etTimeStart.text.toString()
            val timeEnd = etTimeEnd.text.toString()
            val music = etMusic.text.toString()
            val description = etDescription.text.toString()

            if (title.isNotEmpty() && startDate.isNotEmpty()) {
                val jadwal = Jadwal(
                    title = title,
                    startDate = startDate,
                    timeStart = timeStart,
                    timeEnd = timeEnd,
                    music = music,
                    description = description
                )

                lifecycleScope.launch {
                    database.jadwalDao().insertJadwal(jadwal)
                    finish()
                }
            }
        }
    }

    private fun showDatePickerDialog(etStartDate: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            etStartDate.setText(dateFormat.format(selectedDate.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, { _, hour, minute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            editText.setText(formattedTime)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePickerDialog.show()
    }

    private fun openMusicPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == RESULT_OK) {
            data?.data?.let { uri -> etMusic.setText(getFileName(uri)) }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown File"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) fileName = it.getString(index)
            }
        }
        return fileName
    }
}
