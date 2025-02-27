package com.example.agendapersonal

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvGreeting: TextView = findViewById(R.id.tvGreeting)
        val tvDate: TextView = findViewById(R.id.tvDate)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd) // Tambahkan inisialisasi FAB

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val waktu = when {
            currentHour in 5..11 -> "Pagi"
            currentHour in 12..17 -> "Siang"
            else -> "Malam"
        }

        tvGreeting.text = "$waktu, Agendakan Jadwalmu"

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy\nHH:mm", Locale("id", "ID"))
        val formattedDate = dateFormat.format(calendar.time)
        tvDate.text = formattedDate

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Tambahkan event listener untuk tombol FAB
        fabAdd.setOnClickListener {
            val intent = Intent(this, TambahJadwalhal::class.java)
            startActivity(intent)
        }
    }
}
