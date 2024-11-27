/*
in stead of using a mutable list of strings for our habits, we make a habit class
with this class we can also keep track of completed habits
 */

package com.example.habit_tracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits") // create an entity for the database
data class Habit(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var name: String,
    var isChecked: Boolean = false // val cannot be reassigned, var wel
)