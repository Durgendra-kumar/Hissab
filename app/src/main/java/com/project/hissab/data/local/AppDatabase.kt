package com.project.hissab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ExpenseEntity::class, CategoryEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val NAME = "expense_tracker.db"

        /** v1 -> v2: adds the custom categories table. Existing expenses are not touched. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                            "`icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
            }
        }

        /**
         * v2 -> v3: the built-in category list changed, so expenses that used the old six
         * are moved to the closest new category. The ids below are frozen on purpose; don't
         * edit this migration later, add a new one instead.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE expenses SET category_id = 'groceries_food' WHERE category_id = 'grocery'")
                db.execSQL("UPDATE expenses SET category_id = 'transportation' WHERE category_id IN ('transport', 'fuel')")
                db.execSQL("UPDATE expenses SET category_id = 'utilities' WHERE category_id = 'bills'")
                db.execSQL("UPDATE expenses SET category_id = 'entertainment_dining' WHERE category_id = 'dining'")
                db.execSQL("UPDATE expenses SET category_id = 'personal_care_clothing' WHERE category_id = 'shopping'")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}