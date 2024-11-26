package com.example.habit_tracker

import android.icu.text.Transliterator.Position
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper // to implement swipe functionality
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    // save habits and adapter as properties of the activity so we can use them anywhere in the class
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.habitRecyclerView)
        val addHabitButton: FloatingActionButton = findViewById(R.id.addHabitButton)

        // initialize habit list and adapter
        habits = mutableListOf(
            Habit("Drink water"),
            Habit("Exercise"),
            Habit("Read a book")
        )
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
                adapter.removeItem(position) // remove habit from list when swiped to the left
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
                        // add new habit and update list of habits
                        habits.add(Habit(newHabit))
                        adapter.notifyItemInserted(habits.size - 1)
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