package com.example.habit_tracker

import DatePickerFragment
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar


class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // zet de layout
        setContentView(R.layout.activity_main)

        // set up toolbar
        setSupportActionBar(findViewById(R.id.topAppBar))

        // handle dark mode switch
        val darkModeSwitch: Switch = findViewById(R.id.darkModeSwitch)
        // Controleer huidige modus en stel de switch in
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        darkModeSwitch.isChecked = isNightMode

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // handle hamburger menu
        val topAppBar: MaterialToolbar = findViewById(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            // handle menu item click
            Toast.makeText(this, "Menu icon clicked", Toast.LENGTH_SHORT).show()
        }

        // Initialize database and Dao
        database = AppDatabase.getDatabase(this)
        habitDao = database.habitDao()

        val recyclerView: RecyclerView = findViewById(R.id.habitRecyclerView)
        val addHabitButton: FloatingActionButton = findViewById(R.id.addHabitButton)
        val categoryFilterSpinner: Spinner = findViewById(R.id.categoryFilterSpinner)

        // Initialize habit list and adapter
        habits = mutableListOf()
        adapter = HabitRecyclerAdapter(habits, { position ->
            showEditDialog(position)
        }, { habit, isChecked -> // Nieuwe callback
            lifecycleScope.launch {
                if (isChecked) {
                    // Verwijder van Main en voeg toe aan Completed
                    habits.remove(habit)
                    adapter.notifyDataSetChanged()

                    // Start CompletedTasksActivity met de habit
                    val intent = Intent(this@MainActivity, CompletedTasksActivity::class.java)
                    intent.putExtra("habit", habit)
                    startActivity(intent)
                }
            }
        }, habitDao, lifecycleScope)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Load habits from database
        loadHabits()

        // Set up category filter
        val categories = listOf("All", "Work", "Health", "Personal", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = spinnerAdapter

        // Filter habits when a category is selected
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                filterHabits(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        topAppBar.setNavigationOnClickListener {
            // Maak een nieuw venster
            val dialogView = layoutInflater.inflate(R.layout.menu_dialog, null)
            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Stel knoppen in
            val statisticsButton: Button = dialogView.findViewById(R.id.statisticsButton)
            val completedButton: Button = dialogView.findViewById(R.id.completedButton)
            val tasksButton: Button = dialogView.findViewById(R.id.tasksButton) // Nieuwe knop

            // Functies toevoegen (voorlopig leeg)
            statisticsButton.setOnClickListener {
                Toast.makeText(this, "Statistieken", Toast.LENGTH_SHORT).show()
            }

            completedButton.setOnClickListener {
                val intent = Intent(this, CompletedTasksActivity::class.java)
                startActivity(intent)
            }

            tasksButton.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }


            // Toon de dialog
            dialog.show()

            // Pas de breedte en hoogte van het venster aan
            val window = dialog.window
            window?.setLayout(resources.displayMetrics.widthPixels / 2, WindowManager.LayoutParams.MATCH_PARENT)
            window?.setGravity(Gravity.START) // Open vanaf links
            window?.setBackgroundDrawableResource(android.R.color.transparent) // Zorg voor een transparante achtergrond
        }

        // swipe item to the left to delete from list
        val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val habit = habits[position]

                // Verwijder het habit-item uit de database
                lifecycleScope.launch {
                    habitDao.deleteHabit(habit) // Verwijder het item uit de database
                }

                // Verwijder het habit-item uit de lijst en werk de UI bij
                habits.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Add new habit with subtasks
        addHabitButton.setOnClickListener {
            showAddDialog()
        }


    }

    // add new habits
    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val inputField: EditText = dialogView.findViewById(R.id.habitNameInput)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)
        val subtasksContainer: LinearLayout = dialogView.findViewById(R.id.subtasksContainer)
        val addSubtaskButton: Button = dialogView.findViewById(R.id.addSubtaskButton)
        val deleteButton: Button = dialogView.findViewById(R.id.deleteHabitButton)
        val setDeadlineButton: Button = dialogView.findViewById(R.id.setDeadlineButton)
        val deadlineTextView: TextView = dialogView.findViewById(R.id.deadlineTextView)

        // Set up Spinner
        val categories = listOf("Work", "Health", "Personal", "Other")
        val spinnerAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        // Gebruik DatePickerFragment voor de deadline
        setDeadlineButton.setOnClickListener {
            val datePickerFragment = DatePickerFragment { selectedDate ->
                deadlineTextView.text = selectedDate // Stel de geselecteerde datum in
            }
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }

        // Add new subtasks
        addSubtaskButton.setOnClickListener {
            val newSubtaskView = createSubtaskInput("", subtasksContainer)
            subtasksContainer.addView(newSubtaskView)
        }

        // Show dialog
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = inputField.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val subtasks = mutableListOf<Subtask>()
                val deadline = deadlineTextView.text.toString()

                // Retrieve all subtasks
                for (i in 0 until subtasksContainer.childCount) {
                    val container = subtasksContainer.getChildAt(i) as LinearLayout
                    val subtaskInput = container.getChildAt(0) as EditText
                    val subtaskName = subtaskInput.text.toString().trim()
                    if (subtaskName.isNotEmpty()) {
                        subtasks.add(Subtask(subtaskName))
                    }
                }
                if (name.isNotBlank()) {
                    val habit = Habit(name = name, category = category, subtasks = subtasks, deadline = deadline)
                    lifecycleScope.launch {
                        val id = habitDao.insertHabit(habit)
                        habit.id = id.toInt()
                        habits.add(habit)
                        adapter.notifyItemInserted(habits.size - 1)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Habit cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Delete functionality can be removed for "Add Habit"
        deleteButton.visibility = View.GONE

        dialog.show()
    }

    // toon DatePickerDialog
    private fun showDatePickerDialog(habit: Habit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Datum is geselecteerd, sla deze op
                val selectedDate = "$dayOfMonth/${month + 1}/$year" // Datumformaat: dd/mm/yyyy
                habit.deadline = selectedDate
                updateDeadlineTextView(habit)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Werk de TextView bij om de geselecteerde deadline weer te geven
    private fun updateDeadlineTextView(habit: Habit) {
        val deadlineTextView: TextView = findViewById(R.id.deadlineTextView)
        if (habit.deadline != null) {
            deadlineTextView.text = "Deadline: ${habit.deadline}"
        } else {
            deadlineTextView.text = "No deadline set"
        }
    }


    // Load habits from database
    private fun loadHabits() {
        lifecycleScope.launch {
            val habitsFromDb = habitDao.getAllHabits()
            habits.clear()
            habits.addAll(habitsFromDb)
            adapter.notifyDataSetChanged()
        }
    }

    // Filter habits by category
    private fun filterHabits(category: String) {
        lifecycleScope.launch {
            val filteredHabits = if (category == "All") {
                habitDao.getAllHabits() // No filter
            } else {
                habitDao.getHabitsByCategory(category) // Filter by category
            }

            habits.clear()
            habits.addAll(filteredHabits)
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
        val setDeadlineButton: Button = dialogView.findViewById(R.id.setDeadlineButton)
        val deadlineTextView: TextView = dialogView.findViewById(R.id.deadlineTextView)

        // Pre-fill current habit details
        val habit = habits[position]
        inputField.setText(habit.name)

        // set up category spinner
        val categories = listOf("Work", "Health", "Personal", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        categorySpinner.setSelection(categories.indexOf(habit.category))

        // Set up deadline field
        deadlineTextView.text = habit.deadline ?: "No deadline set"

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

        // Handle setting new deadline
        setDeadlineButton.setOnClickListener {
            val datePickerFragment = DatePickerFragment { selectedDate ->
                habit.deadline = selectedDate
                deadlineTextView.text = selectedDate // Update de deadline in het dialog
            }
            datePickerFragment.show(supportFragmentManager, "datePicker")
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
