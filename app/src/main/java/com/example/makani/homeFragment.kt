package com.example.makani

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import java.util.*

class homeFragment : Fragment() {
    private lateinit var notifyBtn: ImageButton
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var databaseRef: DatabaseReference
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private lateinit var allTasks: List<Task>
    private lateinit var filterSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
        fabAddTask = view.findViewById(R.id.fabAddTask)
        notifyBtn = view.findViewById(R.id.notifyBtn)
        filterSpinner = view.findViewById(R.id.filterSpinner)

        tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        taskAdapter = TaskAdapter(taskList, requireContext())
        tasksRecyclerView.adapter = taskAdapter

        databaseRef = FirebaseDatabase.getInstance().getReference("tasks")

        fabAddTask.setOnClickListener {
            startActivity(Intent(requireContext(), Addtask::class.java))
        }

        notifyBtn.setOnClickListener {
            showTasksNotification()
        }

        // Spinner setup
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            filterSpinner.adapter = adapter
        }

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                if (::allTasks.isInitialized) {
                    applyFilter(selected)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadTasks()

        return view
    }

    private fun loadTasks() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                for (snap in snapshot.children) {
                    val task = snap.getValue(Task::class.java)
                    task?.let { tasks.add(it) }
                }
                allTasks = tasks
                applyFilter(filterSpinner.selectedItem.toString())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun applyFilter(selected: String) {
        val filtered = allTasks.filter { task ->
            when (selected) {
                "All" -> true
                "High", "Medium", "Low" -> task.priority.equals(selected, true)
                "Study", "Work", "Personal" -> task.category.equals(selected, true)
                else -> true
            }
        }

        tasksRecyclerView.adapter = TaskAdapter(filtered, requireContext())
    }

    private fun showTasksNotification() {
        val today = Calendar.getInstance().time
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val builder = StringBuilder()

        for (task in taskList) {
            try {
                val dueDateParsed = dateFormat.parse(task.dueDate)
                val diffMillis = dueDateParsed.time - today.time
                val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                val status = when {
                    diffDays > 0 -> "$diffDays Days left"
                    diffDays == 0 -> "Today!"
                    else -> "Overdue"
                }

                builder.append("• ${task.name}: $status\n")
            } catch (e: Exception) {
                builder.append("• ${task.name}: unknown date\n")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Notification")
            .setMessage(builder.toString())
            .setPositiveButton("Okay", null)
            .show()
    }
}
