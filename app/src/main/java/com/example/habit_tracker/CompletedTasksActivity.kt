package com.example.habit_tracker

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class CompletedTasksActivity : AppCompatActivity() {

    private lateinit var habitDao: HabitDao
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_tasks)

        // Initialize database en DAO
        val database = AppDatabase.getDatabase(this)
        habitDao = database.habitDao()

        // Setup RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.completedTasksRecyclerView)
        habits = mutableListOf()
        adapter = HabitRecyclerAdapter(habits, { position ->
            // Eventueel edit-functionaliteit toevoegen
        }, { habit, isChecked -> // Nieuwe callback
            lifecycleScope.launch {
                if (!isChecked) {
                    // Verwijder van Completed en voeg toe aan Main
                    habits.remove(habit)
                    adapter.notifyDataSetChanged()

                    // Start MainActivity met de habit
                    val intent = Intent(this@CompletedTasksActivity, MainActivity::class.java)
                    intent.putExtra("habit", habit)
                    startActivity(intent)
                }
            }
        }, habitDao, lifecycleScope)


        val topAppBar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        topAppBar.setNavigationOnClickListener {
            // Open het menu zoals in MainActivity
            val dialogView = layoutInflater.inflate(R.layout.menu_dialog, null)
            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val statisticsButton: Button = dialogView.findViewById(R.id.statisticsButton)
            val completedButton: Button = dialogView.findViewById(R.id.completedButton)
            val tasksButton: Button = dialogView.findViewById(R.id.tasksButton)

            statisticsButton.setOnClickListener {
                Toast.makeText(this, "Statistieken", Toast.LENGTH_SHORT).show()
            }

            completedButton.setOnClickListener {
                dialog.dismiss()
            }

            tasksButton.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            dialog.show()

            // Pas de breedte en hoogte van het venster aan
            val window = dialog.window
            window?.setLayout(resources.displayMetrics.widthPixels / 2, WindowManager.LayoutParams.MATCH_PARENT)
            window?.setGravity(Gravity.START) // Open vanaf links
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }




        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Laad alleen voltooide taken
        loadCompletedTasks()
    }

    private fun loadCompletedTasks() {
        lifecycleScope.launch {
            val completedHabits = habitDao.getCompletedHabits() // Ophalen van voltooide taken
            habits.clear()
            habits.addAll(completedHabits)
            adapter.notifyDataSetChanged()
        }
    }

}
