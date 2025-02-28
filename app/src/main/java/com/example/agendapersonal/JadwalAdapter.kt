package com.example.agendapersonal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JadwalAdapter(
    private var jadwalList: List<Jadwal>,
    private val onDelete: (Jadwal) -> Unit
) : RecyclerView.Adapter<JadwalAdapter.JadwalViewHolder>() {

    class JadwalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_jadwal, parent, false)
        return JadwalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val jadwal = jadwalList[position]

        // Menampilkan data dalam tampilan
        holder.tvTitle.text = jadwal.title
        holder.tvDateTime.text = "Tanggal: ${jadwal.startDate} | ${jadwal.timeStart} - ${jadwal.timeEnd}"
        holder.tvDescription.text = jadwal.description

        // Aksi tombol hapus
        holder.btnDelete.setOnClickListener {
            onDelete(jadwal)
        }
    }

    override fun getItemCount(): Int = jadwalList.size

    fun updateData(newJadwalList: List<Jadwal>) {
        jadwalList = newJadwalList
        notifyDataSetChanged()
    }
}
