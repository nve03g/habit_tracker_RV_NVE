package com.example.habit_tracker
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subtask(
    val name: String,
    var isComplete: Boolean = false
) : Parcelable

