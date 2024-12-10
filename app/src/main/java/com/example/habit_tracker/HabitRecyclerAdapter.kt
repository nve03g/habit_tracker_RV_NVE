package com.example.habit_tracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout

class HabitRecyclerAdapter(
    private val habits: MutableList<Habit>,
    private val onEditHabit: (Int) -> Unit // Callback to edit habit
) : RecyclerView.Adapter<HabitRecyclerAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitText: TextView = itemView.findViewById(R.id.habitText)
        val habitCategory: TextView = itemView.findViewById(R.id.habitCategory)
        val habitCheckBox: CheckBox = itemView.findViewById(R.id.habitCheckBox)
        val subtasksContainer: LinearLayout = itemView.findViewById(R.id.subtasksContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        // Controleer of de index geldig is
        if (position < 0 || position >= habits.size) return

        val habit = habits[position]

        // Stel de naam en categorie van de hoofdtaak in
        holder.habitText.text = habit.name
        holder.habitCategory.text = habit.category
        holder.habitCheckBox.isChecked = habit.isChecked

        // Verwijder alle oude subtaken (voorkom duplicaten)
        holder.subtasksContainer.removeAllViews()

        // Dynamisch subtaken toevoegen
        habit.subtasks.forEach { subtask ->
            val subtaskCheckBox = CheckBox(holder.itemView.context)
            subtaskCheckBox.text = subtask.name
            subtaskCheckBox.isChecked = subtask.isComplete
            subtaskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                subtask.isComplete = isChecked
                // Optioneel: Update de database of de lijst als nodig
            }
            holder.subtasksContainer.addView(subtaskCheckBox)
        }

        // Hoofdcheckbox logica
        holder.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            habit.isChecked = isChecked
            // Optioneel: Update de database
        }

        // Stel een clicklistener in op de hele hoofdtaak
        holder.itemView.setOnClickListener {
            // Controleer of de index geldig is voordat je een dialoog opent
            if (position < 0 || position >= habits.size) {
                android.widget.Toast.makeText(
                    holder.itemView.context,
                    "Habit not found or already deleted.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Roep een bewerkdialog aan om de hoofdtaak aan te passen
            onEditHabit(position)
        }
    }

    override fun getItemCount(): Int = habits.size

    fun removeItem(position: Int) {
        // Controleer of de index geldig is voordat je een item verwijdert
        if (position >= 0 && position < habits.size) {
            habits.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
