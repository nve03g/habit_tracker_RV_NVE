// initialize database by adding the Singleton
// "In object-oriented programming, the singleton pattern is a software design pattern that restricts the instantiation of a class to a singular instance."
package com.example.habit_tracker

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (instance == null) {
            synchronized(AppDatabase::class) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_database"
                ).build()
            }
        }
        return instance!! // this implies that we know the instance will not be null, so no need to null-check this
    }
}