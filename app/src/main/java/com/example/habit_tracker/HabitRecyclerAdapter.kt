/*
RecyclerView.Adapter is similar to a BaseAdapter, but specifically for a RecyclerView
 */

package com.example.habit_tracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
//import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView

// added to edit habits when clicking on them
import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

class HabitRecyclerAdapter(
    //private val context: Context,
    private val habits: MutableList<Habit>,
    private val onEditHabit: (Int) -> Unit // callback to edit habit
): RecyclerView.Adapter<HabitRecyclerAdapter.HabitViewHolder>() {
    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val habitText: TextView = itemView.findViewById(R.id.habitText)
        val habitCheckBox: CheckBox = itemView.findViewById(R.id.habitCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        holder.habitText.text = habit.name
        holder.habitCheckBox.isChecked = habit.isChecked

        holder.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            habit.isChecked = isChecked
        }

        // click item to trigger edit callback
        holder.itemView.setOnClickListener{
            onEditHabit(position)
            true
        }
    }

    override fun getItemCount(): Int = habits.size

    fun removeItem(position: Int) {
        habits.removeAt(position)
        notifyItemRemoved(position)
    }
}