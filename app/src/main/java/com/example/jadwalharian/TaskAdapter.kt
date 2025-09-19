package com.example.jadwalharian

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TaskAdapter(
    private var tasks: List<Task>,
    // Tombol hapus sudah tidak ada di desain baru, jadi kita hapus onDeleteClick
    // private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Hubungkan semua view dari layout item yang baru
        val card: CardView = itemView.findViewById(R.id.card_task)
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTaskTime)
        val durationTextView: TextView = itemView.findViewById(R.id.textViewTaskDuration)
        val statusTextView: TextView = itemView.findViewById(R.id.textViewTaskStatus)
        val timelineMarker: ImageView = itemView.findViewById(R.id.timeline_marker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val context = holder.itemView.context

        // Set data ke view
        holder.titleTextView.text = task.title
        holder.durationTextView.text = task.duration
        holder.statusTextView.text = task.status

        // Format waktu (09:00 - 09:30)
        val startTime = Date(task.timestamp)
        val durationMillis = parseDurationToMillis(task.duration)
        val endTime = Date(task.timestamp + durationMillis)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timeTextView.text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"

        // Logika untuk mengubah warna dan ikon berdasarkan status
        when (task.status) {
            "Done" -> {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.task_color_done_bg))
                holder.statusTextView.background = ContextCompat.getDrawable(context, R.drawable.status_background_done)
                holder.timelineMarker.setImageResource(R.drawable.ic_timeline_check) // Ikon centang
            }
            "In Progress" -> {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.task_color_progress_bg))
                holder.statusTextView.background = ContextCompat.getDrawable(context, R.drawable.status_background_inprogress)
                holder.timelineMarker.setImageResource(R.drawable.ic_timeline_circle) // Ikon lingkaran biru
                holder.timelineMarker.setColorFilter(ContextCompat.getColor(context, R.color.task_color_progress_icon))
            }
            "Upcoming" -> {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.task_color_upcoming_bg))
                holder.statusTextView.background = ContextCompat.getDrawable(context, R.drawable.status_background_upcoming)
                holder.timelineMarker.setImageResource(R.drawable.ic_timeline_circle) // Ikon lingkaran abu-abu
                holder.timelineMarker.clearColorFilter()
            }
            else -> {
                holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.task_color_upcoming_bg))
                holder.statusTextView.background = ContextCompat.getDrawable(context, R.drawable.status_background_upcoming)
                holder.timelineMarker.setImageResource(R.drawable.ic_timeline_circle)
                holder.timelineMarker.clearColorFilter()
            }
        }
    }

    // Fungsi helper untuk mengkonversi durasi string ke milidetik
    private fun parseDurationToMillis(duration: String): Long {
        return try {
            val parts = duration.split(" ")
            val value = parts[0].toFloat()
            when (parts[1].lowercase()) {
                "minutes" -> TimeUnit.MINUTES.toMillis(value.toLong())
                "hour", "hours" -> (value * TimeUnit.HOURS.toMillis(1)).toLong()
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun getItemCount() = tasks.size
}
