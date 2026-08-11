package io.github.stream29.dashvoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.data.DashVoiceSettings
import io.github.stream29.dashvoice.presentation.RecognitionPhase
import io.github.stream29.dashvoice.presentation.SettingsUiState
import io.github.stream29.dashvoice.ui.theme.DashVoiceTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onVadThresholdChanged: (String) -> Unit,
    onSilenceDurationMillisChanged: (String) -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onSave: () -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
    onOpenVoiceInputSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )

            StatusSection(
                state = state,
                onRequestMicrophonePermission = onRequestMicrophonePermission,
            )

            ConfigurationSection(
                state = state,
                onApiKeyChanged = onApiKeyChanged,
                onBaseUrlChanged = onBaseUrlChanged,
                onToggleApiKeyVisibility = onToggleApiKeyVisibility,
                onVadThresholdChanged = onVadThresholdChanged,
                onSilenceDurationMillisChanged = onSilenceDurationMillisChanged,
                onSave = onSave,
            )

            TestSection(
                state = state,
                onStartTest = onStartTest,
                onStopTest = onStopTest,
            )

            SetupSection(onOpenVoiceInputSettings)
        }
    }
}

@Composable
private fun StatusSection(
    state: SettingsUiState,
    onRequestMicrophonePermission: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.section_status)) {
        if (state.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.status_loading_configuration))
            }
        } else {
            StatusLine(
                label = stringResource(R.string.status_dashscope_configuration),
                ready = state.settings.isReady,
                readyText = stringResource(R.string.status_ready),
                missingText = stringResource(R.string.status_incomplete),
            )
            StatusLine(
                label = stringResource(R.string.status_microphone_permission),
                ready = state.microphonePermissionGranted,
                readyText = stringResource(R.string.status_granted),
                missingText = stringResource(R.string.status_not_granted),
            )
            if (!state.microphonePermissionGranted) {
                OutlinedButton(
                    onClick = onRequestMicrophonePermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_grant_microphone))
                }
            }
            Text(
                text = stringResource(
                    R.string.status_model,
                    DashVoiceSettings.MODEL,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    ready: Boolean,
    readyText: String,
    missingText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(
            text = if (ready) readyText else missingText,
            color = if (ready) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ConfigurationSection(
    state: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onVadThresholdChanged: (String) -> Unit,
    onSilenceDurationMillisChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val vadThreshold = state.vadThresholdInput.toDoubleOrNull()
    val isVadThresholdValid = vadThreshold != null &&
        vadThreshold in DashVoiceSettings.MIN_VAD_THRESHOLD..
        DashVoiceSettings.MAX_VAD_THRESHOLD
    val silenceDurationMillis = state.silenceDurationMillisInput.toIntOrNull()
    val isSilenceDurationValid = silenceDurationMillis != null &&
        silenceDurationMillis in
        DashVoiceSettings.MIN_SILENCE_DURATION_MILLIS..
        DashVoiceSettings.MAX_SILENCE_DURATION_MILLIS

    SettingsSection(title = stringResource(R.string.section_dashscope_configuration)) {
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChanged,
            modifier = Modifier
                .fillMaxWidth()
                .saveOnFocusLost(onSave),
            enabled = !state.isLoading,
            label = { Text("API Key") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            visualTransformation = if (state.apiKeyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = onToggleApiKeyVisibility) {
                    Text(
                        stringResource(
                            if (state.apiKeyVisible) {
                                R.string.action_hide
                            } else {
                                R.string.action_show
                            },
                        ),
                    )
                }
            },
        )

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = onBaseUrlChanged,
            modifier = Modifier
                .fillMaxWidth()
                .saveOnFocusLost(onSave),
            enabled = !state.isLoading,
            label = { Text("Base URL") },
            placeholder = {
                Text("wss://…/api-ws/v1/realtime")
            },
            supportingText = {
                Text(stringResource(R.string.base_url_help))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Text(
            text = stringResource(R.string.language_automatic),
            style = MaterialTheme.typography.titleSmall,
        )

        Text(
            text = stringResource(R.string.vad_settings_title),
            style = MaterialTheme.typography.titleSmall,
        )

        OutlinedTextField(
            value = state.silenceDurationMillisInput,
            onValueChange = onSilenceDurationMillisChanged,
            modifier = Modifier
                .fillMaxWidth()
                .saveOnFocusLost(onSave),
            enabled = !state.isLoading,
            isError = !isSilenceDurationValid,
            label = { Text(stringResource(R.string.vad_silence_duration_label)) },
            supportingText = {
                Text(stringResource(R.string.vad_silence_duration_help))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        OutlinedTextField(
            value = state.vadThresholdInput,
            onValueChange = onVadThresholdChanged,
            modifier = Modifier
                .fillMaxWidth()
                .saveOnFocusLost(onSave),
            enabled = !state.isLoading,
            isError = !isVadThresholdValid,
            label = { Text(stringResource(R.string.vad_threshold_label)) },
            supportingText = {
                Text(stringResource(R.string.vad_threshold_help))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
        )

        state.statusMessageRes?.let {
            Text(
                text = stringResource(it),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TestSection(
    state: SettingsUiState,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.section_recognition_test)) {
        Text(
            text = state.testRecognition.localizedTitle(),
            style = MaterialTheme.typography.titleMedium,
        )
        state.testRecognition.localizedGuidance()?.let { guidance ->
            Text(
                text = guidance,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.testRecognition.isActive) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.testRecognition.transcript.isNotBlank()) {
            Text(
                text = state.testRecognition.transcript,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        state.testRecognition.languageTag?.let {
            Text(
                text = stringResource(R.string.detected_language, it),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onStartTest,
                modifier = Modifier.weight(1f),
                enabled = !state.testRecognition.isActive && !state.isSaving,
            ) {
                Text(
                    stringResource(
                        if (state.testRecognition.phase == RecognitionPhase.RESULT) {
                            R.string.action_test_again
                        } else {
                            R.string.action_start_test
                        },
                    ),
                )
            }
            OutlinedButton(
                onClick = onStopTest,
                modifier = Modifier.weight(1f),
                enabled = state.testRecognition.phase == RecognitionPhase.LISTENING ||
                    state.testRecognition.phase == RecognitionPhase.SPEAKING,
            ) {
                Text(stringResource(R.string.action_finish))
            }
        }
    }
}

@Composable
private fun SetupSection(onOpenVoiceInputSettings: () -> Unit) {
    SettingsSection(title = stringResource(R.string.section_connect_ime)) {
        Text(stringResource(R.string.setup_step_1))
        Text(stringResource(R.string.setup_step_2))
        Text(stringResource(R.string.setup_step_3))
        OutlinedButton(
            onClick = onOpenVoiceInputSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_open_voice_input_settings))
        }
    }
}

private fun Modifier.saveOnFocusLost(onSave: () -> Unit): Modifier =
    onFocusChanged { focusState ->
        if (!focusState.isFocused) {
            onSave()
        }
    }

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )
        content()
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    DashVoiceTheme {
        SettingsScreen(
            state = SettingsUiState(
                isLoading = false,
                apiKey = "sk-example",
                baseUrl = "wss://workspace.ap-southeast-1.maas.aliyuncs.com/api-ws/v1/realtime",
                microphonePermissionGranted = true,
            ),
            onApiKeyChanged = {},
            onBaseUrlChanged = {},
            onToggleApiKeyVisibility = {},
            onVadThresholdChanged = {},
            onSilenceDurationMillisChanged = {},
            onRequestMicrophonePermission = {},
            onSave = {},
            onStartTest = {},
            onStopTest = {},
            onOpenVoiceInputSettings = {},
        )
    }
}
