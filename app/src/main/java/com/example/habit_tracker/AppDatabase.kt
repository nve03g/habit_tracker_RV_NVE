package com.example.habit_tracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Habit::class], version = 5, exportSchema = false) // Verhoogde versie
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Voeg de nieuwe kolom deadline toe
                database.execSQL("ALTER TABLE habits ADD COLUMN deadline TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Voeg de nieuwe kolom imageUri toe aan de habits tabel
                database.execSQL("ALTER TABLE habits ADD COLUMN imageUri TEXT")
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
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // Optioneel: verwijder bestaande gegevens bij schemawijziging
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
