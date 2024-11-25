package com.example.habit_tracker

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    // save habits and adapter as properties of the activity so we can use them anywhere in the class
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val habitListView: ListView = findViewById(R.id.habitListView)
        val addHabitButton: Button = findViewById(R.id.addHabitButton)

        // initialize habit list and adapter
        habits = mutableListOf(
            Habit("drink water"),
            Habit("exercise"),
            Habit("read a book")
        )
        adapter = HabitAdapter(this, habits)

        habitListView.adapter = adapter

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
                        adapter.notifyDataSetChanged()
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
}