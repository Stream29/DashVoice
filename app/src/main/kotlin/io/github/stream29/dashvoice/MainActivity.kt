package io.github.stream29.dashvoice

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.stream29.dashvoice.presentation.SettingsEvent
import io.github.stream29.dashvoice.presentation.SettingsViewModel
import io.github.stream29.dashvoice.ui.SettingsScreen
import io.github.stream29.dashvoice.ui.theme.DashVoiceTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        (application as DashVoiceApplication).container.viewModelFactory
    }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onMicrophonePermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.events.collect(::handleEvent)
            }

            DashVoiceTheme {
                SettingsScreen(
                    state = uiState,
                    onApiKeyChanged = viewModel::onApiKeyChanged,
                    onBaseUrlChanged = viewModel::onBaseUrlChanged,
                    onToggleApiKeyVisibility = viewModel::toggleApiKeyVisibility,
                    onVadThresholdChanged = viewModel::onVadThresholdChanged,
                    onSilenceDurationMillisChanged =
                        viewModel::onSilenceDurationMillisChanged,
                    onRequestMicrophonePermission = viewModel::requestMicrophonePermission,
                    onSave = viewModel::save,
                    onStartTest = viewModel::startTest,
                    onStopTest = viewModel::stopTest,
                    onOpenVoiceInputSettings = viewModel::openVoiceInputSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onMicrophonePermissionChanged(
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.RequestMicrophonePermission -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.onMicrophonePermissionResult(true)
                } else {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            SettingsEvent.OpenVoiceInputSettings -> {
                val voiceInputSettings = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                try {
                    startActivity(voiceInputSettings)
                } catch (_: ActivityNotFoundException) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}
