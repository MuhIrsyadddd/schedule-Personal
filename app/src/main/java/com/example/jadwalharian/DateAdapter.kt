package com.example.jadwalharian

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

// Data class yang hilang, sekarang ada di sini
data class DateItem(
    val date: Date,
    var isSelected: Boolean = false
)

class DateAdapter(private val dates: List<DateItem>) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    // Lacak posisi item yang sedang dipilih
    private var selectedPosition = dates.indexOfFirst { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val dateItem = dates[position]
        holder.bind(dateItem)

        // Atur listener klik untuk setiap item tanggal
        holder.itemView.setOnClickListener {
            if (selectedPosition != holder.adapterPosition) {
                // Hapus status terpilih dari item lama
                if (selectedPosition != -1) {
                    dates[selectedPosition].isSelected = false
                    notifyItemChanged(selectedPosition)
                }

                // Set status terpilih untuk item baru yang diklik
                selectedPosition = holder.adapterPosition
                dates[selectedPosition].isSelected = true
                notifyItemChanged(selectedPosition)

                // TODO: Di sini Anda bisa menambahkan logika untuk memfilter daftar tugas
                // berdasarkan tanggal yang dipilih.
            }
        }
    }

    override fun getItemCount(): Int = dates.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayName: TextView = itemView.findViewById(R.id.textViewDayName)
        private val dayNumber: TextView = itemView.findViewById(R.id.textViewDayNumber)

        fun bind(dateItem: DateItem) {
            val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

            dayName.text = dayNameFormat.format(dateItem.date).uppercase()
            dayNumber.text = dayNumberFormat.format(dateItem.date)

            // Mengubah tampilan item (latar belakang & warna teks) berdasarkan status isSelected
            itemView.isSelected = dateItem.isSelected
        }
    }
}

