package com.example.habit_tracker

import android.media.Image
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.children
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HabitRecyclerAdapter(
    private val habits: MutableList<Habit>,
    private val onEditHabit: (Int) -> Unit,
    private val onHabitMoved: (Habit, Boolean) -> Unit, // Nieuwe callback
    private val habitDao: HabitDao,
    private val lifecycleScope: CoroutineScope
) : RecyclerView.Adapter<HabitRecyclerAdapter.HabitViewHolder>() {

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitText: TextView = itemView.findViewById(R.id.habitText)
        val habitCategory: TextView = itemView.findViewById(R.id.habitCategory)
        val habitCheckBox: CheckBox = itemView.findViewById(R.id.habitCheckBox)
        val subtasksContainer: LinearLayout = itemView.findViewById(R.id.subtasksContainer)
        val deadlineTextView: TextView = itemView.findViewById(R.id.deadlineTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        // Stel de naam, categorie en checkbox in
        holder.habitText.text = habit.name
        holder.habitCategory.text = habit.category
        holder.habitCheckBox.setOnCheckedChangeListener(null) // Verwijder oude listener
        holder.habitCheckBox.isChecked = habit.isChecked

        // Voeg de deadline toe aan de UI
        val deadlineText = habit.deadline ?: "No deadline"
        holder.deadlineTextView.text = deadlineText

        // voeg image toe
        val habitImageView: ImageView = holder.itemView.findViewById(R.id.habitImageView)
        try {
            if (habit.imageUri != null && habit.imageUri!!.isNotBlank()) {
                habitImageView.setImageURI(Uri.parse(habit.imageUri))
            } else {
                habitImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            habitImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }


        // Update visuele status
        updateStrikeThrough(holder, habit.isChecked)

        // Stel nieuwe listener in
        holder.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            habit.isChecked = isChecked
            updateStrikeThrough(holder, isChecked)

            // Verwijder uit huidige lijst en voeg toe aan de andere
            onHabitMoved(habit, isChecked)

            // Update de database
            lifecycleScope.launch {
                habitDao.updateHabit(habit)
            }
        }


        // Dynamisch subtaken verwerken
        if (habit.subtasks.isNotEmpty()) {
            holder.subtasksContainer.visibility = View.VISIBLE
            holder.subtasksContainer.removeAllViews()
            habit.subtasks.forEach { subtask ->
                val subtaskCheckBox = CheckBox(holder.itemView.context).apply {
                    text = subtask.name
                    isChecked = subtask.isComplete
                    paintFlags = if (isChecked) {
                        paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                    setOnCheckedChangeListener { _, subtaskChecked ->
                        subtask.isComplete = subtaskChecked
                        paintFlags = if (subtaskChecked) {
                            paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                        // Update de database na wijziging
                        lifecycleScope.launch {
                            habitDao.updateHabit(habit)
                        }
                    }
                }
                holder.subtasksContainer.addView(subtaskCheckBox)
            }
        } else {
            holder.subtasksContainer.visibility = View.GONE
        }

        // Stel een clicklistener in op de hele hoofdtaak
        holder.itemView.setOnClickListener {
            onEditHabit(position)
        }


        // hoofdtaak checkbox logica
        holder.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            habit.isChecked = isChecked
            updateStrikeThrough(holder, isChecked)

            // Synchroniseer de status van de subtaken
            habit.subtasks.forEach { subtask ->
                subtask.isComplete = isChecked
            }

            // Werk de UI bij voor de subtaken
            holder.subtasksContainer.children.forEach { view ->
                if (view is CheckBox) {
                    view.isChecked = isChecked
                }
            }

            // Update de habit in de database
            lifecycleScope.launch {
                habitDao.updateHabit(habit)
            }
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
    private fun updateStrikeThrough(holder: HabitViewHolder, isChecked: Boolean) {
        if (isChecked) {
            holder.habitText.paintFlags = holder.habitText.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.habitCategory.paintFlags = holder.habitCategory.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.habitText.paintFlags = holder.habitText.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.habitCategory.paintFlags = holder.habitCategory.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

}
