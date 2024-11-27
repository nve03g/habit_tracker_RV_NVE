package com.example.habit_tracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

// gebruik WorkManager om herinneringen te plannen
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// notificatiekanaal definiëren
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build


class MainActivity : AppCompatActivity() {
    // save habits and adapter as properties of the activity so we can use them anywhere in the class
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

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
                        // stel herinnering in, bv 1 dag later
                        scheduleReminder(newHabit,1440)
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

        // Spinner for repetition of a habit
        val repetitionSpinner = android.widget.Spinner(this)
        val repetitionOptions = Repetition.entries.map { it.name.replaceFirstChar { char -> char.uppercase() } }
        val spinnerAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            repetitionOptions
        )

        repetitionSpinner.adapter = spinnerAdapter
        repetitionSpinner.setSelection(habits[position].repetition.ordinal)

        // layout for the dialog pop-up window
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(inputField)
            addView(repetitionSpinner)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Edit habit")
            .setMessage("Update your habit")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val updatedHabit = inputField.text.toString()
                val selectedRepetition = Repetition.entries[repetitionSpinner.selectedItemPosition]
                if(updatedHabit.isNotBlank()){
                    habits[position].name = updatedHabit
                    habits[position].repetition = selectedRepetition
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

    private fun scheduleReminder(habitName: String, delayInMinutes: Long) {
        // gegevens die naar de Worker gestuurd worden
        val data = Data.Builder()
            .putString("habit_name", habitName)
            .build()

        // taak plannen
        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delayInMinutes, TimeUnit.MINUTES) // pas hier de timing aan
            .build()

        // voeg de taak toe aan WorkManager
        WorkManager.getInstance(this).enqueue(reminderRequest)
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                "habit_reminders",
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Herinneringen om je gewoontes te voltooien"
            }

            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}