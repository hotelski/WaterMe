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

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasPlants = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'plants'",
            ).use { cursor -> cursor.moveToFirst() }

            if (hasPlants) {
                db.execSQL("ALTER TABLE plants ADD COLUMN environment TEXT NOT NULL DEFAULT 'INDOOR'")
            }
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_care_advice_cache` (
                    `cache_key` TEXT NOT NULL,
                    `plant_id` TEXT,
                    `plant_name` TEXT NOT NULL,
                    `scientific_name` TEXT,
                    `generated_at` INTEGER NOT NULL,
                    `model_name` TEXT NOT NULL,
                    `advice_plant_name` TEXT NOT NULL,
                    `advice_scientific_name` TEXT,
                    `short_description` TEXT NOT NULL,
                    `care_difficulty` TEXT NOT NULL,
                    `mature_height` TEXT NOT NULL,
                    `watering` TEXT NOT NULL,
                    `light` TEXT NOT NULL,
                    `temperature` TEXT NOT NULL,
                    `humidity` TEXT NOT NULL,
                    `fertilizing` TEXT NOT NULL,
                    `repotting` TEXT NOT NULL,
                    `flowering` TEXT NOT NULL,
                    `growth` TEXT NOT NULL,
                    `toxicity` TEXT NOT NULL,
                    `origin` TEXT NOT NULL,
                    `disclaimer` TEXT NOT NULL,
                    `suggested_watering_interval_days` INTEGER,
                    `suggested_fertilizing_interval_days` INTEGER,
                    `suggested_light_level` TEXT,
                    `suggested_note` TEXT,
                    PRIMARY KEY(`cache_key`),
                    FOREIGN KEY(`plant_id`) REFERENCES `plants`(`plant_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_ai_care_advice_cache_plant_id`
                ON `ai_care_advice_cache` (`plant_id`)
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
    )
}
