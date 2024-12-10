package com.example.habit_tracker

data class Subtask(
    val name: String,            // Naam van de subtask
    var isComplete: Boolean = false // Voltooiingsstatus
)
