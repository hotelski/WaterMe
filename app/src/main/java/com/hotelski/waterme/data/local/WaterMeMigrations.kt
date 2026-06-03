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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasCareHistory = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'care_history'",
            ).use { cursor -> cursor.moveToFirst() }

            if (hasCareHistory) {
                db.execSQL("ALTER TABLE care_history ADD COLUMN photo_uri TEXT")
            }
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasPlants = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'plants'",
            ).use { cursor -> cursor.moveToFirst() }

            if (hasPlants) {
                db.execSQL("ALTER TABLE plants ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
