package com.example.habit_tracker

import DatePickerFragment
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
//import android.icu.util.Calendar
import java.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.habit_tracker.network.QuotesRepository
//import com.example.habittracker.data.repository.QuotesRepository
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.text.ParseException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var habits: MutableList<Habit>
    private lateinit var adapter: HabitRecyclerAdapter

    private lateinit var pickImageLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent) // Redirect user to allow scheduling alarms
            }
        }

        // Controleer en vraag POST_NOTIFICATIONS-toestemming
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // zet de layout
        setContentView(R.layout.activity_main)

        // Plan dagelijkse notificatie om 9u met WorkManager
        val quotesRepository = QuotesRepository()
        scheduleDailyQuote()

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

        // initialize image launcher
        try {

            pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
                    selectedImageUri = result.data?.data
                    // Werk de ImageView bij
                    currentImageView?.setImageURI(selectedImageUri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing image picker", Toast.LENGTH_SHORT).show()
        }



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
                val intent = Intent(this, StatisticsActivity::class.java)
                startActivity(intent)
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

    private fun scheduleDailyQuote() {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 19)
            set(Calendar.SECOND, 0)
        }

        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<QuoteWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyQuoteWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    private fun sendDailyNotification(quote: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_quote_channel"

        // Maak een notificatiekanaal aan voor Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dagelijkse Motivatie",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dagelijkse Motivatie")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote)) // Voor langere teksten
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Toestemming voor meldingen toegestaan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Meldingen zijn geweigerd", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setNotification(habit: Habit) {
        if (habit.deadline.isNullOrBlank()) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        try {
            val deadlineDate = sdf.parse(habit.deadline)?.time ?: return
            val triggerTime = deadlineDate - 24 * 60 * 60 * 1000 // 1 day before

            if (triggerTime < System.currentTimeMillis()) return // Skip past deadlines

            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("habit_name", habit.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                habit.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use exact alarm if permissions are granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent) // Fallback for approximate alarm
            }
        } catch (e: ParseException) {
            Log.e("SetNotification", "Invalid date format: ${habit.deadline}", e)
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

        val habitImageView: ImageView = dialogView.findViewById(R.id.habitImageView)
        val selectImageButton: Button = dialogView.findViewById(R.id.selectImageButton)

        // Select Image knop logica
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Voorkom crash bij een null URI
        habitImageView.setImageURI(selectedImageUri ?: Uri.parse("android.resource://$packageName/drawable/ic_menu_gallery"))

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
                    Log.d("HabitDebug", "Selected Image URI: ${selectedImageUri?.toString() ?: "null"}")

                    val habit = Habit(
                        name = name,
                        category = category,
                        subtasks = subtasks,
                        deadline = if (deadline.isNotBlank()) deadline else null, // Use null for unset deadlines
                        imageUri = selectedImageUri?.toString() ?: "" // save image, Zet null om naar lege string
                    )
                    lifecycleScope.launch {
                        val id = habitDao.insertHabit(habit)
                        habit.id = id.toInt()
                        habits.add(habit)
                        setNotification(habit)
                        sendImmediateNotification(habit.name ?: "Unnamed Habit", habit.deadline ?: "Geen deadline ingesteld")
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

    private fun sendImmediateNotification(habitName: String, deadline: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_deadline_channel"

        // Creëer het Notification Channel (voor Android 8.0 en hoger)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Bouw de notificatie
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Habit Deadline Set")
            .setContentText("Deadline voor '$habitName' is ingesteld op $deadline.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Toon de notificatie
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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

    private var currentImageView: ImageView? = null // Huidige ImageView voor edit


    // Function to display edit dialog
    private fun showEditDialog(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val inputField: EditText = dialogView.findViewById(R.id.habitNameInput)
        val categorySpinner: Spinner = dialogView.findViewById(R.id.categorySpinner)
        val addSubtaskButton: Button = dialogView.findViewById(R.id.addSubtaskButton)
        val deleteButton: Button = dialogView.findViewById(R.id.deleteHabitButton)
        val setDeadlineButton: Button = dialogView.findViewById(R.id.setDeadlineButton)
        val deadlineTextView: TextView = dialogView.findViewById(R.id.deadlineTextView)
        val habitImageView: ImageView = dialogView.findViewById(R.id.habitImageView)
        val selectImageButton: Button = dialogView.findViewById(R.id.selectImageButton)
        val subtasksContainer: LinearLayout = dialogView.findViewById(R.id.subtasksContainer)

        val habit = habits[position]
        inputField.setText(habit.name)
        deadlineTextView.text = habit.deadline ?: "No deadline set"

        // Verwijder oude inhoud in de subtasksContainer
        subtasksContainer.removeAllViews()

        // Voeg bestaande subtaken toe aan het container
        habit.subtasks.forEach { subtask ->
            val subtaskView = createSubtaskInput(subtask.name, subtasksContainer).apply {
                val subtaskInput = getChildAt(0) as EditText
                subtaskInput.setText(subtask.name)
            }
            subtasksContainer.addView(subtaskView)
        }

        // Toon bestaande afbeelding, indien aanwezig
        if (habit.imageUri != null && habit.imageUri!!.isNotBlank()) {
            habitImageView.setImageURI(Uri.parse(habit.imageUri))
        } else {
            habitImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Variabele om de nieuwe afbeelding URI bij te houden
        var newImageUri: Uri? = null

        // Gebruik de bestaande pickImageLauncher
        selectImageButton.setOnClickListener {
            currentImageView = habitImageView // Update de huidige ImageView
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

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

        // Spinner-logica
        val categories = listOf("Work", "Health", "Personal", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categorySpinner.adapter = spinnerAdapter
        categorySpinner.setSelection(categories.indexOf(habit.category))

        // Positive button om wijzigingen op te slaan
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = inputField.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val updatedSubtasks = mutableListOf<Subtask>()
                val deadline = deadlineTextView.text.toString()

                // haal alle subtaken op
                for (i in 0 until subtasksContainer.childCount) {
                    val container = subtasksContainer.getChildAt(i) as LinearLayout
                    val subtaskInput = container.getChildAt(0) as EditText
                    val subtaskName = subtaskInput.text.toString().trim()
                    if (subtaskName.isNotEmpty()) {
                        // Controleer of de subtask al bestaat in de originele lijst
                        val existingSubtask = habit.subtasks.find { it.name == subtaskName }
                        updatedSubtasks.add(
                            existingSubtask ?: Subtask(subtaskName) // Behoud de originele status of maak een nieuwe
                        )
                    }
                }
                if (name.isNotBlank()) {
                    habit.name = name
                    habit.category = category
                    habit.subtasks = updatedSubtasks
                    habit.deadline = deadline
                    habit.imageUri = newImageUri?.toString() ?: habit.imageUri // Bewaar nieuwe afbeelding indien gekozen

                    lifecycleScope.launch {
                        habitDao.updateHabit(habit)
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
