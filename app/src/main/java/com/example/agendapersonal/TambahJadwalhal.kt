package com.example.agendapersonal

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TambahJadwalhal : AppCompatActivity() {
    private lateinit var database: JadwalDatabase
    private lateinit var tvSelectedMusic: TextView
    private var selectedMusicUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_jadwalhal)

        database = JadwalDatabase.getDatabase(this)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etStartDate = findViewById<EditText>(R.id.etStartDate)
        val etTimeStart = findViewById<EditText>(R.id.etTimeStart)
        val etTimeEnd = findViewById<EditText>(R.id.etTimeEnd)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnSelectMusic = findViewById<LinearLayout>(R.id.btnSelectMusic)
        tvSelectedMusic = findViewById(R.id.tvSelectedMusic)
        val btnDone = findViewById<Button>(R.id.btnDone)

        etStartDate.setOnClickListener { showDatePickerDialog(etStartDate) }
        etTimeStart.setOnClickListener { showTimePickerDialog(etTimeStart) }
        etTimeEnd.setOnClickListener { showTimePickerDialog(etTimeEnd) }

        btnSelectMusic.setOnClickListener {
            selectMusicFile()
        }

        btnDone.setOnClickListener {
            val title = etTitle.text.toString()
            val startDate = etStartDate.text.toString()
            val timeStart = etTimeStart.text.toString()
            val timeEnd = etTimeEnd.text.toString()
            val description = etDescription.text.toString()
            val musicPath = selectedMusicUri?.toString() ?: ""

            if (title.isNotEmpty() && startDate.isNotEmpty()) {
                val jadwal = Jadwal(
                    title = title,
                    startDate = startDate,
                    timeStart = timeStart,
                    timeEnd = timeEnd,
                    music = musicPath,
                    description = description
                )

                lifecycleScope.launch {
                    database.jadwalDao().insertJadwal(jadwal)
                    setResult(RESULT_OK)
                    finish()
                }
            } else {
                Toast.makeText(this, "Judul dan Tanggal Mulai tidak boleh kosong", Toast.LENGTH_SHORT).show()
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

    private val selectMusicLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedMusicUri = result.data!!.data
                tvSelectedMusic.text = getFileName(selectedMusicUri!!)
            }
        }

    private fun selectMusicFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        selectMusicLauncher.launch(intent)
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
