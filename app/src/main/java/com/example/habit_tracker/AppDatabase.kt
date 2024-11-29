// define a database class
package com.example.habit_tracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
The error occurs because Room detects a schema change (e.g., adding the category field to the Habit entity)
but does not have instructions for migrating the existing database schema.
Simply increasing the version number (version = 2) is not enough; you must also define a migration strategy.
*/

@Database(entities = [Habit::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) { // tell Room how to handle the transition from version 1 to version 2
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new column `category` with a default value
                database.execSQL("ALTER TABLE habits ADD COLUMN category TEXT DEFAULT 'General' NOT NULL")
            }
        }

        fun getDatabase(context: Context) : AppDatabase {
            // make sure only one instance of the database is created
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_database"
                )
                    .addMigrations(MIGRATION_1_2) // add the migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}