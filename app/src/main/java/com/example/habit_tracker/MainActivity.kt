package com.example.habit_tracker

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper // to implement swipe functionality
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var habitDao: HabitDao
    // save habits and adapter as properties of the activity so we can use them anywhere in the class
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setup Spinner categories
        val categoryFilterSpinner: Spinner = findViewById(R.id.categoryFilterSpinner)
        val filterCategories = listOf("All", "Work", "Health", "Personal", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterCategories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = spinnerAdapter

        // listener to filter habits
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = filterCategories[position]
                filterHabits(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        // initialize database and Dao
        database = AppDatabase.getDatabase(this)
        habitDao = database.habitDao()

        val recyclerView: RecyclerView = findViewById(R.id.habitRecyclerView)
        val addHabitButton: FloatingActionButton = findViewById(R.id.addHabitButton)

        // initialize habit list and adapter
        habits = mutableListOf()
        adapter = HabitRecyclerAdapter(habits) { position ->
            showEditDialog(position) // callback to edit a habit
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    habitDao.deleteHabit(habits[position]) // delete habit from database
                    habits.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // click button to add new habit
        addHabitButton.setOnClickListener{
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
            val inputField : EditText = dialogView.findViewById(R.id.habitNameInput)
            val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)

            // set up Spinner
            val categories = listOf("Work", "Health", "Personal", "Other")
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = spinnerAdapter

            // show dialog
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("New habit")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val name = inputField.text.toString()
                    val category = categorySpinner.selectedItem.toString()
                    if (name.isNotBlank()){
                        val habit = Habit(name = name, category = category)
                        lifecycleScope.launch {
                            val id = habitDao.insertHabit(habit) // save habit in database
                            habit.id = id.toInt() // generate an id for the habit in the database
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

        // load habits from database at startup
        lifecycleScope.launch {
            val habitsFromDb = habitDao.getAllHabits()
            habits.clear()
            habits.addAll(habitsFromDb)
            adapter.notifyDataSetChanged()
        }
    }

    // function to display edit dialog
    private fun showEditDialog(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val inputField : EditText = dialogView.findViewById(R.id.habitNameInput)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)

        // pre-fill current habit details
        inputField.setText(habits[position].name)
        val categories = listOf("Work", "Health", "Personal", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        categorySpinner.setSelection(categories.indexOf(habits[position].category))

        // show dialog
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Edit habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = inputField.text.toString()
                val category = categorySpinner.selectedItem.toString()
                if(name.isNotBlank()){
                    habits[position].name = name
                    habits[position].category = category
                    lifecycleScope.launch {
                        habitDao.updateHabit(habits[position]) // update habit in database
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
        dialog.show()
    }

    private fun filterHabits(category: String) {
        lifecycleScope.launch {
            val filteredHabits = if (category == "All") {
                habitDao.getAllHabits()
            } else {
                habitDao.getHabitsByCategory(category)
            }
            habits.clear()
            habits.addAll(filteredHabits)
            adapter.notifyDataSetChanged()
        }
    }

}