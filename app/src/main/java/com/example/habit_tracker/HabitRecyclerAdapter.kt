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

class HabitRecyclerAdapter(
    private val habits: MutableList<Habit>
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
    }

    override fun getItemCount(): Int = habits.size

    fun removeItem(position: Int) {
        habits.removeAt(position)
        notifyItemRemoved(position)
    }
}