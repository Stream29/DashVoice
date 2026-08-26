package io.github.stream29.dashvoice.data

import kotlinx.coroutines.CancellationException

interface ConfigurationRepository {
    suspend fun load(): DashVoiceSettings
    suspend fun save(settings: DashVoiceSettings): Boolean
}

class RoomConfigurationRepository(
    private val configurationDao: ConfigurationDao,
) : ConfigurationRepository {

    override suspend fun load(): DashVoiceSettings =
        configurationDao.load()?.toSettings() ?: DashVoiceSettings()

    override suspend fun save(settings: DashVoiceSettings): Boolean {
        val normalizedSettings = settings.copy(
            apiKey = settings.apiKey.trim(),
            baseUrl = settings.baseUrl.trim(),
        )
        if (!normalizedSettings.isReady) return false

        return try {
            configurationDao.save(normalizedSettings.toEntity())
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            false
        }
    }

    private fun ConfigurationEntity.toSettings(): DashVoiceSettings =
        DashVoiceSettings(
            apiKey = apiKey,
            baseUrl = baseUrl,
            vadThreshold = vadThreshold,
            silenceDurationMillis = silenceDurationMillis,
            removeTrailingSentencePunctuation = removeTrailingSentencePunctuation,
            removeSpacesAtCjkBoundaries = removeSpacesAtCjkBoundaries,
            semanticPunctuationEnabled = semanticPunctuationEnabled,
        )

    private fun DashVoiceSettings.toEntity(): ConfigurationEntity =
        ConfigurationEntity(
            apiKey = apiKey,
            baseUrl = baseUrl,
            language = AUTOMATIC_LANGUAGE,
            vadPreset = CUSTOM_VAD,
            vadThreshold = vadThreshold,
            silenceDurationMillis = silenceDurationMillis,
            removeTrailingSentencePunctuation = removeTrailingSentencePunctuation,
            removeSpacesAtCjkBoundaries = removeSpacesAtCjkBoundaries,
            semanticPunctuationEnabled = semanticPunctuationEnabled,
        )

    private companion object {
        const val AUTOMATIC_LANGUAGE = "AUTO"
        const val CUSTOM_VAD = "CUSTOM"
    }
}
