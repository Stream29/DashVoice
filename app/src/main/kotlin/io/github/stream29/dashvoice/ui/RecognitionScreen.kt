package io.github.stream29.dashvoice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.presentation.RecognitionPhase
import io.github.stream29.dashvoice.presentation.RecognitionUiState
import io.github.stream29.dashvoice.ui.theme.DashVoiceTheme

@Composable
fun RecognitionScreen(
    state: RecognitionUiState,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(48.dp))
                VoiceOrb(
                    rms = state.rms,
                    active = state.phase == RecognitionPhase.LISTENING ||
                        state.phase == RecognitionPhase.SPEAKING,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = state.localizedTitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                state.localizedGuidance()?.let { guidance ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = guidance,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                if (state.transcript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = state.transcript,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                state.languageTag?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.detected_language, it),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                when (state.phase) {
                    RecognitionPhase.LISTENING,
                    RecognitionPhase.SPEAKING -> Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_finish))
                    }

                    RecognitionPhase.ERROR -> Button(
                        onClick = if (state.isConfigurationError) {
                            onOpenSettings
                        } else {
                            onRetry
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (state.isConfigurationError) {
                                    R.string.action_open_settings
                                } else {
                                    R.string.action_retry
                                },
                            ),
                        )
                    }

                    RecognitionPhase.CONNECTING,
                    RecognitionPhase.PROCESSING,
                    RecognitionPhase.IDLE,
                    RecognitionPhase.RESULT -> Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VoiceOrb(
    rms: Float,
    active: Boolean,
) {
    val outerSize = (136 + rms.coerceIn(0f, 10f) * 3).dp
    Box(
        modifier = Modifier.size(170.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(outerSize)
                .clip(CircleShape)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecognitionScreenPreview() {
    DashVoiceTheme {
        RecognitionScreen(
            state = RecognitionUiState(
                phase = RecognitionPhase.SPEAKING,
                titleRes = R.string.recognition_title_speaking,
                guidanceRes = R.string.recognition_guidance_auto_finish,
                transcript = "Realtime recognition text",
                languageTag = "zh-CN",
                rms = 6f,
            ),
            onStop = {},
            onRetry = {},
            onOpenSettings = {},
            onCancel = {},
        )
    }
}
