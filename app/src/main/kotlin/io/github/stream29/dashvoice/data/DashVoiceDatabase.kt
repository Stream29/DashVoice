package io.github.stream29.dashvoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [ConfigurationEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class DashVoiceDatabase : RoomDatabase() {
    abstract fun configurationDao(): ConfigurationDao

    companion object {
        @Volatile
        private var instance: DashVoiceDatabase? = null

        fun getInstance(context: Context): DashVoiceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DashVoiceDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }

        private const val DATABASE_NAME = "dashvoice.db"

        private val MIGRATION_1_2 = Migration(1, 2) { database ->
            database.execSQL(
                "ALTER TABLE configuration " +
                    "ADD COLUMN vad_threshold REAL NOT NULL DEFAULT 0.0",
            )
            database.execSQL(
                "ALTER TABLE configuration " +
                    "ADD COLUMN silence_duration_ms INTEGER NOT NULL DEFAULT 400",
            )
            database.execSQL(
                """
                UPDATE configuration
                SET vad_threshold = CASE vad_preset
                    WHEN 'BALANCED' THEN 0.2
                    ELSE 0.0
                END,
                silence_duration_ms = CASE vad_preset
                    WHEN 'BALANCED' THEN 800
                    ELSE 400
                END
                """.trimIndent(),
            )
        }

        private val MIGRATION_2_3 = Migration(2, 3) { database ->
            database.execSQL(
                "ALTER TABLE configuration " +
                    "ADD COLUMN remove_trailing_sentence_punctuation " +
                    "INTEGER NOT NULL DEFAULT 1",
            )
            database.execSQL(
                "ALTER TABLE configuration " +
                    "ADD COLUMN remove_spaces_at_cjk_boundaries " +
                    "INTEGER NOT NULL DEFAULT 1",
            )
        }

        private val MIGRATION_3_4 = Migration(3, 4) { database ->
            database.execSQL(
                "ALTER TABLE configuration " +
                    "ADD COLUMN semantic_punctuation_enabled INTEGER NOT NULL DEFAULT 1",
            )
        }
    }
}
