package io.github.stream29.dashvoice

import android.content.Context
import io.github.stream29.dashvoice.data.ConfigurationRepository
import io.github.stream29.dashvoice.data.DashVoiceDatabase
import io.github.stream29.dashvoice.data.RoomConfigurationRepository
import io.github.stream29.dashvoice.recognition.AndroidSpeechRecognitionGateway
import io.github.stream29.dashvoice.recognition.SpeechRecognitionGateway

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val database by lazy {
        DashVoiceDatabase.getInstance(applicationContext)
    }

    val configurationRepository: ConfigurationRepository by lazy {
        RoomConfigurationRepository(database.configurationDao())
    }

    val viewModelFactory: DashVoiceViewModelFactory by lazy {
        DashVoiceViewModelFactory(this)
    }

    fun createSpeechRecognitionGateway(): SpeechRecognitionGateway =
        AndroidSpeechRecognitionGateway(applicationContext)
}
