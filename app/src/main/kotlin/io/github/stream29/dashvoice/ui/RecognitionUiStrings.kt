package io.github.stream29.dashvoice.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.stream29.dashvoice.presentation.RecognitionUiState

@Composable
internal fun RecognitionUiState.localizedTitle(): String =
    stringResource(titleRes)

@Composable
internal fun RecognitionUiState.localizedGuidance(): String? {
    val resource = guidanceRes ?: return null
    return diagnosticCode?.let { stringResource(resource, it) }
        ?: stringResource(resource)
}
