// add an interface for database operations
// Dao = data access object
package com.example.habit_tracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    suspend fun getAllHabits(): List<Habit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Query("SELECT * FROM habits WHERE category = :category")
    suspend fun getHabitsByCategory(category: String): List<Habit>

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
}