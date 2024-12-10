package com.example.habit_tracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Habit::class], version = 3, exportSchema = false) // Verhoogde versie
@TypeConverters(SubtaskConverter::class) // Registreer de TypeConverter voor subtaken
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migratie van versie 2 naar 3 (geen wijzigingen aan tabellen vereist voor subtaken)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Voeg de nieuwe kolom subtasks toe
                database.execSQL("ALTER TABLE habits ADD COLUMN subtasks TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_database" // Naam van je database
                )
                    .addMigrations(MIGRATION_2_3) // Voeg migraties toe
                    .fallbackToDestructiveMigration() // Optioneel: verwijder bestaande gegevens bij schemawijziging
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
