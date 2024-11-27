package com.example.habit_tracker

import android.os.Bundle
import android.widget.EditText
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
            val inputField = android.widget.EditText(this).apply {
                hint = "Enter new habit"
            }

            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("New habit")
                .setMessage("What habit would you like to add?")
                .setView(inputField)
                .setPositiveButton("Add") { _, _ ->
                    val newHabit = inputField.text.toString()
                    if (newHabit.isNotBlank()){
                        val habit = Habit(name = newHabit) // pass the name parameter
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
            habits.addAll(habitDao.getAllHabits())
            adapter.notifyDataSetChanged()
        }
    }

    // function to display edit dialog
    private fun showEditDialog(position: Int) {
        val inputField = android.widget.EditText(this).apply {
            hint = "Edit habit"
            setText(habits[position].name) // pre-fill with current habit name
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Edit habit")
            .setMessage("Update your habit")
            .setView(inputField)
            .setPositiveButton("Save") { _, _ ->
                val updatedHabit = inputField.text.toString()
                if(updatedHabit.isNotBlank()){
                    habits[position].name = updatedHabit
                    lifecycleScope.launch {
                        habitDao.updateHabit(habits[position]) // update habit in database
                    }
                    adapter.notifyItemChanged(position)
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
}