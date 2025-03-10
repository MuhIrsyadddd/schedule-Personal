package com.example.agendapersonal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(
    private var alarmList: MutableList<AlarmData>,
    private val onDeleteClick: (AlarmData) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJudul: TextView = itemView.findViewById(R.id.tvJudul)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvDeskripsi: TextView = itemView.findViewById(R.id.tvDeskripsi)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarmList[position]
        holder.tvJudul.text = alarm.judul
        holder.tvTanggal.text = alarm.tanggal
        holder.tvTime.text = "${alarm.hour}:${String.format("%02d", alarm.minute)}"
        holder.tvDeskripsi.text = alarm.deskripsi

        holder.btnDelete.setOnClickListener {
            onDeleteClick(alarm)
        }
    }

    override fun getItemCount() = alarmList.size

    fun removeItem(alarm: AlarmData) {
        val position = alarmList.indexOf(alarm)
        if (position != -1) {
            alarmList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getAlarmList(): List<AlarmData> = alarmList
}
