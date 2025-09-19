package com.example.jadwalharian

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class DateItem(
    val date: Date,
    var isSelected: Boolean = false
)

// PERBARUI KONSTRUKTOR: Tambahkan onDateClick
class DateAdapter(
    private val dates: List<DateItem>,
    private val onDateClick: (Date) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = dates.indexOfFirst { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val dateItem = dates[position]
        holder.bind(dateItem)

        holder.itemView.setOnClickListener {
            if (selectedPosition != holder.adapterPosition) {
                if (selectedPosition != -1) {
                    dates[selectedPosition].isSelected = false
                    notifyItemChanged(selectedPosition)
                }

                selectedPosition = holder.adapterPosition
                dates[selectedPosition].isSelected = true
                notifyItemChanged(selectedPosition)

                // PERBARUI LOGIKA: Panggil listener dengan tanggal yang dipilih
                onDateClick(dateItem.date)
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

            dayName.text = dayNameFormat.format(dateItem.date).uppercase(Locale.getDefault())
            dayNumber.text = dayNumberFormat.format(dateItem.date)

            itemView.isSelected = dateItem.isSelected
        }
    }
}