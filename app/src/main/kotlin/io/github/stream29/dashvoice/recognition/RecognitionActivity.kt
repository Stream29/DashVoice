package io.github.stream29.dashvoice.recognition

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.stream29.dashvoice.DashVoiceApplication
import io.github.stream29.dashvoice.MainActivity
import io.github.stream29.dashvoice.presentation.RecognitionEvent
import io.github.stream29.dashvoice.presentation.RecognitionViewModel
import io.github.stream29.dashvoice.ui.RecognitionScreen
import io.github.stream29.dashvoice.ui.theme.DashVoiceTheme

class RecognitionActivity : ComponentActivity() {
    private val viewModel: RecognitionViewModel by viewModels {
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
        onBackPressedDispatcher.addCallback(this) {
            viewModel.cancel()
        }
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.events.collect(::handleEvent)
            }

            DashVoiceTheme {
                RecognitionScreen(
                    state = uiState,
                    onStop = viewModel::stop,
                    onRetry = viewModel::retry,
                    onOpenSettings = viewModel::openSettings,
                    onCancel = viewModel::cancel,
                )
            }
        }
        viewModel.prepare()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHostResumed()
    }

    private fun handleEvent(event: RecognitionEvent) {
        when (event) {
            RecognitionEvent.RequestMicrophonePermission -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.onMicrophonePermissionResult(true)
                } else {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            RecognitionEvent.OpenSettings -> {
                startActivity(Intent(this, MainActivity::class.java))
            }

            is RecognitionEvent.FinishWithResults -> {
                setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        putStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS,
                            ArrayList(event.results),
                        )
                        putExtra(
                            SpeechRecognizer.CONFIDENCE_SCORES,
                            FloatArray(event.results.size) { -1f },
                        )
                    },
                )
                finish()
            }

            RecognitionEvent.FinishCanceled -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }
}
