package io.github.stream29.dashvoice.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.presentation.RecognitionPhase
import io.github.stream29.dashvoice.presentation.RecognitionUiState
import io.github.stream29.dashvoice.ui.theme.DashVoiceTheme

@Composable
fun VoiceImeScreen(
    state: RecognitionUiState,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(220.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_keyboard),
                        contentDescription = stringResource(R.string.ime_return_keyboard),
                        modifier = Modifier.size(26.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.ime_open_settings),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                VoiceActionButton(
                    state = state,
                    onStop = onStop,
                )
            }
        }
    }
}

@Composable
private fun VoiceActionButton(
    state: RecognitionUiState,
    onStop: () -> Unit,
) {
    val canStop = state.phase == RecognitionPhase.LISTENING ||
        state.phase == RecognitionPhase.SPEAKING
    val speakingScale = if (state.phase == RecognitionPhase.SPEAKING) {
        1f + state.rms.coerceIn(0f, 10f) * 0.008f
    } else {
        1f
    }
    val scale by animateFloatAsState(
        targetValue = speakingScale,
        label = "Voice action scale",
    )
    val containerColor = when (state.phase) {
        RecognitionPhase.CONNECTING ->
            MaterialTheme.colorScheme.secondaryContainer

        RecognitionPhase.PROCESSING ->
            MaterialTheme.colorScheme.tertiaryContainer

        RecognitionPhase.ERROR ->
            MaterialTheme.colorScheme.errorContainer

        else ->
            MaterialTheme.colorScheme.primary
    }
    val contentColor = when (state.phase) {
        RecognitionPhase.CONNECTING ->
            MaterialTheme.colorScheme.onSecondaryContainer

        RecognitionPhase.PROCESSING ->
            MaterialTheme.colorScheme.onTertiaryContainer

        RecognitionPhase.ERROR ->
            MaterialTheme.colorScheme.onErrorContainer

        else ->
            MaterialTheme.colorScheme.onPrimary
    }
    val showsProgress = state.phase == RecognitionPhase.CONNECTING ||
        state.phase == RecognitionPhase.PROCESSING

    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showsProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(116.dp),
                color = contentColor,
                trackColor = containerColor,
                strokeWidth = 4.dp,
            )
        }
        FilledIconButton(
            onClick = onStop,
            enabled = canStop,
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor,
            ),
        ) {
            Icon(
                painter = painterResource(
                    if (canStop) {
                        R.drawable.ic_stop
                    } else {
                        R.drawable.ic_mic
                    },
                ),
                contentDescription = if (canStop) {
                    stringResource(R.string.ime_stop_voice_input)
                } else {
                    stringResource(R.string.ime_voice_input_status)
                },
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceImeScreenPreview() {
    DashVoiceTheme {
        VoiceImeScreen(
            state = RecognitionUiState(
                phase = RecognitionPhase.SPEAKING,
                titleRes = R.string.recognition_title_speaking,
                rms = 6f,
            ),
            onStop = {},
            onCancel = {},
            onOpenSettings = {},
        )
    }
}
