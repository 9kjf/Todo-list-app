package com.example.makani

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class Addtask : AppCompatActivity() {
    private lateinit var colorSpinner: Spinner
    private lateinit var taskNameEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var prioritySpinner: Spinner
    private lateinit var dueDateTextView: TextView
    private lateinit var addTaskButton: Button
    private lateinit var editSubtask: EditText
    private lateinit var btnAddSubtask: Button
    private lateinit var listSubtasks: ListView
    private val subtasks = mutableListOf<String>()
    private lateinit var subtaskAdapter: ArrayAdapter<String>
    private lateinit var recurrenceSpinner: Spinner
    private var recurrence: String = "None"

    private var dueDateRaw = ""
    private var taskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask)

        colorSpinner = findViewById(R.id.colorSpinner)
        taskNameEditText = findViewById(R.id.taskNameEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        prioritySpinner = findViewById(R.id.prioritySpinner)
        dueDateTextView = findViewById(R.id.dueDateTextView)
        addTaskButton = findViewById(R.id.addTaskButton)
        editSubtask = findViewById(R.id.editSubtask)
        btnAddSubtask = findViewById(R.id.btnAddSubtask)
        listSubtasks = findViewById(R.id.listSubtasks)

        subtaskAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subtasks)
        listSubtasks.adapter = subtaskAdapter

        recurrenceSpinner = findViewById(R.id.recurrenceSpinner)

        val recurrenceOptions = listOf("None", "Daily", "Weekly", "Monthly")
        val recurrenceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, recurrenceOptions)
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        recurrenceSpinner.adapter = recurrenceAdapter

        recurrenceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                recurrence = recurrenceOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        btnAddSubtask.setOnClickListener {
            val subtaskText = editSubtask.text.toString().trim()
            if (subtaskText.isNotEmpty()) {
                subtasks.add(subtaskText)
                subtaskAdapter.notifyDataSetChanged()
                editSubtask.text.clear()
            }
        }

        val categoryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.task_categories,
            android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        val priorityAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.task_priorities,
            android.R.layout.simple_spinner_item
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        prioritySpinner.adapter = priorityAdapter

        // لو في تعديل نعبّي البيانات
        taskId = intent.getStringExtra("taskId")
        if (taskId != null) {
            taskNameEditText.setText(intent.getStringExtra("taskName"))
            dueDateRaw = intent.getStringExtra("taskDueDate") ?: ""
            dueDateTextView.text = "تم اختيار تاريخ سابق"

            val category = intent.getStringExtra("taskCategory")
            val priority = intent.getStringExtra("taskPriority")
            val incomingSubtasks = intent.getStringArrayListExtra("subtasks") ?: arrayListOf()
            subtasks.addAll(incomingSubtasks)
            subtaskAdapter.notifyDataSetChanged()

            val catPos = categoryAdapter.getPosition(category)
            val priPos = priorityAdapter.getPosition(priority)

            if (catPos >= 0) categorySpinner.setSelection(catPos)
            if (priPos >= 0) prioritySpinner.setSelection(priPos)

            addTaskButton.text = "Edit Task"
        }

        dueDateTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)

                    dueDateRaw = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

                    val today = Calendar.getInstance()
                    val diffMillis = selectedCalendar.timeInMillis - today.timeInMillis
                    val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                    dueDateTextView.text = when {
                        diffDays > 0 -> "باقي $diffDays يوم"
                        diffDays == 0 -> "اليوم هو الموعد النهائي!"
                        else -> "انتهى موعد هذه المهمة"
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        addTaskButton.setOnClickListener {
            val taskName = taskNameEditText.text.toString()
            val category = categorySpinner.selectedItem.toString()
            val priority = prioritySpinner.selectedItem.toString()
            val dueDate = dueDateRaw
            val bgColor = colorSpinner.selectedItem.toString()
            if (taskName.isNotEmpty() && dueDate.isNotEmpty()) {
                val db = FirebaseDatabase.getInstance().getReference("tasks")
                val recurrenceType = recurrenceSpinner.selectedItem.toString()
                val currentId = taskId ?: db.push().key ?: return@setOnClickListener
                val task = Task(
                    id = currentId,
                    name = taskName,
                    category = category,
                    priority = priority,
                    dueDate = dueDate,
                    backgroundColor = bgColor,
                    isDone = false, // لأنه مهمة جديدة
                    recurrence = recurrenceType, // تأكدي أنك عرفتيها مثلاً من Spinner أو غيره
                    subtasks = subtasks
                )


                db.child(currentId).setValue(task).addOnSuccessListener {
                    val message = if (taskId == null) "Task added successfully!" else "Task is updated!"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    onBackPressedDispatcher.onBackPressed()
                }.addOnFailureListener {
                    Toast.makeText(this, "Fail: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter the titel and the date", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
