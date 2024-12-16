package com.example.habit_tracker


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String,
    var category: String = "General",
    var isChecked: Boolean = false,
    var subtasks: MutableList<Subtask> = mutableListOf() // Maak Subtask ook Parcelable
) : Parcelable

