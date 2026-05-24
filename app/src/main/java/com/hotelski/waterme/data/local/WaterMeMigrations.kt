package com.hotelski.waterme.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object WaterMeMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasUserSettings = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'user_settings'",
            ).use { cursor -> cursor.moveToFirst() }

            if (hasUserSettings) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN last_synced_at INTEGER")
            }
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasCareHistory = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'care_history'",
            ).use { cursor -> cursor.moveToFirst() }

            if (hasCareHistory) {
                db.execSQL("ALTER TABLE care_history ADD COLUMN health_mood TEXT")
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
