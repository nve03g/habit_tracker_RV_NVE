package com.example.habit_tracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database and Dao
        database = AppDatabase.getDatabase(this)
        habitDao = database.habitDao()

        val recyclerView: RecyclerView = findViewById(R.id.habitRecyclerView)
        val addHabitButton: FloatingActionButton = findViewById(R.id.addHabitButton)

        // Initialize habit list and adapter
        habits = mutableListOf()
        adapter = HabitRecyclerAdapter(habits) { position ->
            showEditDialog(position) // Callback to edit a habit
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    habitDao.deleteHabit(habits[position]) // Delete habit from database
                    habits.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Add new habit with subtasks
        addHabitButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
            val inputField: EditText = dialogView.findViewById(R.id.habitNameInput)
            val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)

            // Set up Spinner
            val categories = listOf("Work", "Health", "Personal", "Other")
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = spinnerAdapter

            // Show dialog
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("New Habit")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val name = inputField.text.toString()
                    val category = categorySpinner.selectedItem.toString()
                    val subtasks = mutableListOf<Subtask>() // Lege subtakenlijst

                    if (name.isNotBlank()) {
                        val habit = Habit(name = name, category = category, subtasks = subtasks)
                        lifecycleScope.launch {
                            val id = habitDao.insertHabit(habit) // Save habit in database
                            habit.id = id.toInt()
                            habits.add(habit)
                            adapter.notifyItemInserted(habits.size - 1)
                        }
                    } else {
                        android.widget.Toast.makeText(
                            this,
                            "Habit cannot be empty",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
            dialog.show()
        }

        // Load habits from database at startup
        lifecycleScope.launch {
            val habitsFromDb = habitDao.getAllHabits()
            habits.clear()
            habits.addAll(habitsFromDb)
            adapter.notifyDataSetChanged()
        }
    }

    // Function to display edit dialog
    private fun showEditDialog(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val inputField: EditText = dialogView.findViewById(R.id.habitNameInput)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)
        val subtasksContainer: LinearLayout = dialogView.findViewById(R.id.subtasksContainer)
        val addSubtaskButton: Button = dialogView.findViewById(R.id.addSubtaskButton)
        val deleteButton: Button = dialogView.findViewById(R.id.deleteHabitButton)

        // Pre-fill current habit details
        val habit = habits[position]
        inputField.setText(habit.name)
        val categories = listOf("Work", "Health", "Personal", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        categorySpinner.setSelection(categories.indexOf(habit.category))

        // Dynamisch subtaken invoegen
        subtasksContainer.removeAllViews()
        habit.subtasks.forEach { subtask ->
            val subtaskView = createSubtaskInput(subtask.name, subtasksContainer)
            subtasksContainer.addView(subtaskView)
        }

        // Voeg nieuwe subtaken toe
        addSubtaskButton.setOnClickListener {
            val newSubtaskView = createSubtaskInput("", subtasksContainer)
            subtasksContainer.addView(newSubtaskView)
        }

        // Show dialog
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = inputField.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val updatedSubtasks = mutableListOf<Subtask>()

                // Haal alle subtaken op
                for (i in 0 until subtasksContainer.childCount) {
                    val container = subtasksContainer.getChildAt(i) as LinearLayout
                    val subtaskInput = container.getChildAt(0) as EditText
                    val subtaskName = subtaskInput.text.toString().trim()
                    if (subtaskName.isNotEmpty()) {
                        updatedSubtasks.add(Subtask(subtaskName))
                    }
                }

                if (name.isNotBlank()) {
                    habit.name = name
                    habit.category = category
                    habit.subtasks = updatedSubtasks
                    lifecycleScope.launch {
                        habitDao.updateHabit(habit) // Update habit in database
                        adapter.notifyItemChanged(position)
                    }
                } else {
                    android.widget.Toast.makeText(
                        this,
                        "Habit cannot be empty",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Verwijder habit functionaliteit
        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                habitDao.deleteHabit(habit)
                habits.removeAt(position)
                adapter.notifyItemRemoved(position)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // Hulpmethode om een nieuw invoerveld met verwijderknop te maken
    private fun createSubtaskInput(initialText: String, subtasksContainer: LinearLayout): LinearLayout {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val subtaskInput = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "Subtask"
            setText(initialText)
        }

        val deleteButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(android.R.drawable.ic_menu_delete)
            setOnClickListener {
                subtasksContainer.removeView(container) // Verwijder de hele rij
            }
        }

        container.addView(subtaskInput)
        container.addView(deleteButton)

        return container
    }
}
