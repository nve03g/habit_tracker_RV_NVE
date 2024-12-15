package com.example.habit_tracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.Toast

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

        // subtaken verwerken
        if (habit.subtasks.isNotEmpty()) {
            holder.subtasksContainer.visibility = View.VISIBLE
            holder.subtasksContainer.removeAllViews() // voorkom duplicaten
            habit.subtasks.forEach { subtask ->
                val subtaskCheckBox = CheckBox(holder.itemView.context).apply {
                    text = subtask.name
                    isChecked = subtask.isComplete
                    setOnCheckedChangeListener { _, isChecked ->
                        subtask.isComplete = isChecked
                        // update in database? (optioneel)
                    }
                }
                holder.subtasksContainer.addView(subtaskCheckBox)
            }
        } else {
            holder.subtasksContainer.visibility = View.GONE
        }

        // hoofdtaak checkbox logica
        holder.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            habit.isChecked = isChecked
            // update in database? (optioneel)
        }

        // Stel een clicklistener in op de hele hoofdtaak (bewerk-functionaliteit)
        holder.itemView.setOnClickListener {
            // Controleer of de index geldig is voordat je een dialoog opent
            if (position < 0 || position >= habits.size) {
                Toast.makeText(
                    holder.itemView.context,
                    "Habit not found or already deleted.",
                    Toast.LENGTH_SHORT
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
        if (position in 0 until habits.size) {
            habits.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
