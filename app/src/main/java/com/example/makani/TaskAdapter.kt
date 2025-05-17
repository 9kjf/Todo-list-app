package com.example.makani

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

data class Task(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val priority: String = "",
    val dueDate: String = "",
    val backgroundColor: String = "",
    val isDone: Boolean = false,
    val recurrence: String = "None",
    var subtasks: List<String> = listOf()
)

class TaskAdapter(private val taskList: List<Task>, private val context: android.content.Context) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskName: TextView = view.findViewById(R.id.taskNameTextView)
        val taskCategory: TextView = view.findViewById(R.id.taskCategoryTextView)
        val taskPriority: TextView = view.findViewById(R.id.taskPriorityTextView)
        val dueDate: TextView = view.findViewById(R.id.taskDueDateTextView)
        val checkBox: CheckBox = view.findViewById(R.id.taskCheckBox)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteTaskButton)
        val editBtn: ImageButton = view.findViewById(R.id.editTaskButton)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskName.text = task.name
        holder.taskCategory.text = task.category
        holder.taskPriority.text = task.priority



       //count
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val dueDateParsed = dateFormat.parse(task.dueDate)
            val today = Calendar.getInstance().time

            dueDateParsed?.let {
                val diffMillis = it.time - today.time
                val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                val displayText = when {
                    diffDays > 0 -> "$diffDays Days left"
                    diffDays == 0 -> "Today"
                    else -> "overdue"
                }

                holder.dueDate.text = displayText
            } ?: run {
                holder.dueDate.text = task.dueDate
            }

        } catch (e: Exception) {
            holder.dueDate.text = task.dueDate
        }

        // التشيك بوكس من Firebase

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.isDone

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val dbRef = FirebaseDatabase.getInstance().getReference("tasks").child(task.id)
            dbRef.child("isDone").setValue(isChecked).addOnSuccessListener {
                val msg = if (isChecked) "Task marked as Done ✔️" else "Task marked as not done"
                Toast.makeText(holder.itemView.context, msg, Toast.LENGTH_SHORT).show()
                if (isChecked && task.recurrence != "None") {
                    val newId = FirebaseDatabase.getInstance().getReference("tasks").push().key!!
                    val newDueDate = calculateNextDate(task.dueDate, task.recurrence)
                    val repeatedTask = task.copy(id = newId, isDone = false, dueDate = newDueDate)

                    FirebaseDatabase.getInstance().getReference("tasks").child(newId).setValue(repeatedTask)
                }

            }.addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Failed to update task status", Toast.LENGTH_SHORT).show()
            }
        }




        // خلفية حسب اللون
        when (task.backgroundColor) {
            "Pink" -> holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.pink))
            "Blue" -> holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
            "Green" -> holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            else -> holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }


        // حذف المهمة
        holder.deleteBtn.setOnClickListener {
            val dbRef = FirebaseDatabase.getInstance().getReference("tasks")
            dbRef.child(task.id).removeValue().addOnSuccessListener {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(context, "Fail to delete", Toast.LENGTH_SHORT).show()
            }
        }


        holder.editBtn.setOnClickListener {
            val intent = Intent(context, Addtask::class.java).apply {
                putExtra("taskId", task.id)
                putExtra("taskName", task.name)
                putExtra("taskCategory", task.category)
                putExtra("taskPriority", task.priority)
                putExtra("taskDueDate", task.dueDate)
                putExtra("isDone", task.isDone)
                putExtra("backgroundColor", task.backgroundColor)
            }
            context.startActivity(intent)
        }
        val subtasksContainer = holder.itemView.findViewById<LinearLayout>(R.id.tvSubtasks)
        subtasksContainer.removeAllViews() // مسح القديم كل مرة

        if (task.subtasks.isNotEmpty()) {
            for (subtask in task.subtasks) {
                val textView = TextView(context)
                textView.text = "• $subtask"
                textView.setTextColor(ContextCompat.getColor(context, R.color.black))
                textView.textSize = 14f
                subtasksContainer.addView(textView)
            }
        } else {
            val noSubtasks = TextView(context)
            noSubtasks.text = "No subtasks"
            noSubtasks.setTextColor(ContextCompat.getColor(context, R.color.white))
            noSubtasks.textSize = 14f
            subtasksContainer.addView(noSubtasks)
        }


    }

    override fun getItemCount(): Int = taskList.size
    private fun calculateNextDate(currentDate: String, recurrence: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            calendar.time = sdf.parse(currentDate) ?: return currentDate
        } catch (e: Exception) {
            return currentDate
        }

        when (recurrence) {
            "Daily" -> calendar.add(Calendar.DATE, 1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> calendar.add(Calendar.MONTH, 1)
        }

        return sdf.format(calendar.time)
    }

}
