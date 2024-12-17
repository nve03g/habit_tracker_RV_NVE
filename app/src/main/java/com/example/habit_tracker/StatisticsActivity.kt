package com.example.habit_tracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    private lateinit var habitDao: HabitDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // initialiseer de database en Dao
        val database = AppDatabase.getDatabase(this)
        habitDao = database.habitDao()

        // roep de functie aan om de grafiek in te stellen
        setupCompletionChart()


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
                dialog.dismiss()
            }

            completedButton.setOnClickListener {
                val intent = Intent(this, CompletedTasksActivity::class.java)
                startActivity(intent)
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
    }

    private fun setupCompletionChart() {
        lifecycleScope.launch {
            // Haal alle habits op vanuit de database
            val allHabits = habitDao.getAllHabits()

            // Bereken het aantal voltooide en onvoltooide taken
            val completedCount = allHabits.count { it.isChecked }
            val uncompletedCount = allHabits.size - completedCount

            // Stel de data in voor een horizontale stacked bar chart
            val stackedEntry = BarEntry(0f, floatArrayOf(completedCount.toFloat(), uncompletedCount.toFloat()))

            // Maak een dataset en voeg stijlen toe
            val dataSet = BarDataSet(listOf(stackedEntry), "")
            dataSet.colors = listOf(Color.GREEN, Color.RED) // Groen en rood voor de verschillende delen
            dataSet.stackLabels = arrayOf("Voltooid", "Onvoltooid") // Labels voor de gestapelde delen

            // Pas de waardeformattering aan (geen kommagetallen)
            dataSet.valueFormatter = object : ValueFormatter() {
                override fun getBarStackedLabel(value: Float, stackedEntry: BarEntry?): String {
                    return value.toInt().toString() // Geef alleen hele getallen terug
                }
            }
            dataSet.valueTextSize = 14f // Verhoog de tekstgrootte van de barlabels

            val barData = BarData(dataSet)
            barData.barWidth = 0.8f // Breder maken voor duidelijkheid

            // Configureer de grafiek
            val completionChart: HorizontalBarChart = findViewById(R.id.completionChart)
            completionChart.data = barData
            completionChart.description.isEnabled = false // Geen beschrijving
            completionChart.setFitBars(true) // Zorg dat de balk goed past

            // Zorg dat labels ONDER de bars getekend worden
            completionChart.setDrawValueAboveBar(false)

            completionChart.setExtraOffsets(10f, 10f, 10f, 20f) // Extra ruimte rond de grafiek

            // X-as (voor labels)
            val xAxis = completionChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.setDrawLabels(false) // Geen extra labels op X-as nodig

            // Y-as
            val leftAxis = completionChart.axisLeft
            leftAxis.setDrawLabels(false) // Geen labels links
            leftAxis.setDrawGridLines(false)
            leftAxis.axisMinimum = 0f

            val rightAxis = completionChart.axisRight
            rightAxis.isEnabled = false // Geen rechter Y-as

            // Legende: aanpassen om overlappen te voorkomen
            val legend = completionChart.legend
            legend.isEnabled = true
            legend.textSize = 12f
            legend.form = Legend.LegendForm.SQUARE

            // Ruimte tussen de legendelabels en de vormen
            legend.xEntrySpace = 30f // Verhoog de horizontale ruimte tussen elementen
            legend.yEntrySpace = 3f // Verhoog de verticale ruimte (hoeft vaak niet veel te veranderen bij horizontale orientatie)
            //legend.formToTextSpace = 10f // Verhoog de ruimte tussen de kleurvorm en de tekst

            completionChart.invalidate() // Vernieuw de grafiek
        }
    }



}