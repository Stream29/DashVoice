package io.github.stream29.dashvoice.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuration")
data class ConfigurationEntity(
    @PrimaryKey
    @ColumnInfo(name = "singleton_id")
    val singletonId: Int = SINGLETON_ID,
    @ColumnInfo(name = "api_key")
    val apiKey: String,
    @ColumnInfo(name = "base_url")
    val baseUrl: String,
    @ColumnInfo(name = "language")
    val language: String,
    @ColumnInfo(name = "vad_preset")
    val vadPreset: String,
    @ColumnInfo(name = "vad_threshold", defaultValue = "0.0")
    val vadThreshold: Double,
    @ColumnInfo(name = "silence_duration_ms", defaultValue = "400")
    val silenceDurationMillis: Int,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
