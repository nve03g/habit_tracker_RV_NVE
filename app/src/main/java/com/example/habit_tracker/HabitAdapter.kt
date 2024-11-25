/*
we need an adapter to show the habit objects in the list with checkboxes
*/

package com.example.habit_tracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.BaseAdapter

class HabitAdapter(
    private val context: Context,
    private val habits: MutableList<Habit>
): BaseAdapter() {
    override fun getCount(): Int = habits.size

    override fun getItem(position: Int): Any = habits[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.list_item_habit,
            parent,
            false
        )

        val habit = habits[position]

        val habitText: TextView = view.findViewById(R.id.habitText)
        val habitCheckBox: CheckBox = view.findViewById(R.id.habitCheckBox)

        habitText.text = habit.name
        habitCheckBox.isChecked = habit.isChecked

        habitCheckBox.setOnCheckedChangeListener {_, isChecked ->
            habit.isChecked = isChecked
        }

        return view
    }
}