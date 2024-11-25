package com.example.habit_tracker

import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

/*
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
*/

class MainActivity : AppCompatActivity() {
    // save habits and adapter as properties of the activity so we can use them anywhere in the class
    private lateinit var habits: MutableList<String>
    private lateinit var adapter: android.widget.ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        */

        val habitListView: ListView = findViewById(R.id.habitListView)
        val addHabitButton: Button = findViewById(R.id.addHabitButton)

        // dummy list data
        val habits = mutableListOf("drink water", "exercise", "read a book")
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            habits
        )
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
                        habits.add(newHabit)
                        adapter.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
            dialog.show()
        }
    }
}