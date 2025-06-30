package com.example.jadwalharian

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var tasks: List<Task>,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTaskTitle)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTaskTime)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete_task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleTextView.text = task.title
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy - HH:mm", Locale.getDefault())
        holder.timeTextView.text = sdf.format(Date(task.timestamp))

        holder.deleteButton.setOnClickListener {
            onDeleteClick(task)
        }
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun getItemCount() = tasks.size
}