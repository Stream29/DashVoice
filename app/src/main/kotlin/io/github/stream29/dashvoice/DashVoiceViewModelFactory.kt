package io.github.stream29.dashvoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.stream29.dashvoice.ime.VoiceImeViewModel
import io.github.stream29.dashvoice.presentation.RecognitionViewModel
import io.github.stream29.dashvoice.presentation.SettingsViewModel

class DashVoiceViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T = when {
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(
                configurationRepository = container.configurationRepository,
                speechRecognitionGateway = container.createSpeechRecognitionGateway(),
            ) as T

        modelClass.isAssignableFrom(RecognitionViewModel::class.java) ->
            RecognitionViewModel(
                configurationRepository = container.configurationRepository,
                speechRecognitionGateway = container.createSpeechRecognitionGateway(),
            ) as T

        modelClass.isAssignableFrom(VoiceImeViewModel::class.java) ->
            VoiceImeViewModel(
                speechRecognitionGateway = container.createSpeechRecognitionGateway(),
            ) as T

        else -> error("Unsupported ViewModel: ${modelClass.name}")
    }
}
