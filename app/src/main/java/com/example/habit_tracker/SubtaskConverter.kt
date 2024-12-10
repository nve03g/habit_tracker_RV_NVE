package com.example.habit_tracker

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SubtaskConverter {

    @TypeConverter
    fun fromSubtaskList(subtasks: MutableList<Subtask>): String {
        return Gson().toJson(subtasks)
    }

    @TypeConverter
    fun toSubtaskList(data: String): MutableList<Subtask> {
        val listType = object : TypeToken<MutableList<Subtask>>() {}.type
        return Gson().fromJson(data, listType)
    }
}
