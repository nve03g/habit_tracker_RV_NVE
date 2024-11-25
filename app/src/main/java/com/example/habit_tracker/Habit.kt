/*
in stead of using a mutable list of strings for our habits, we make a habit class
with this class we can also keep track of completed habits
 */

package com.example.habit_tracker

data class Habit(
    val name: String,
    var isChecked: Boolean = false // val cannot be reassigned, var wel
)