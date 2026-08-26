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
    @ColumnInfo(name = "remove_trailing_sentence_punctuation", defaultValue = "1")
    val removeTrailingSentencePunctuation: Boolean,
    @ColumnInfo(name = "remove_spaces_at_cjk_boundaries", defaultValue = "1")
    val removeSpacesAtCjkBoundaries: Boolean,
    @ColumnInfo(name = "semantic_punctuation_enabled", defaultValue = "1")
    val semanticPunctuationEnabled: Boolean,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
