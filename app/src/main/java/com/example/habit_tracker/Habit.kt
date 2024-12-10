package com.example.habit_tracker


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "habits") // Create an entity for the database
data class Habit(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String,
    var category: String = "General",
    var isChecked: Boolean = false,
    var subtasks: MutableList<Subtask> = mutableListOf() // Lijst met subtaken

)

