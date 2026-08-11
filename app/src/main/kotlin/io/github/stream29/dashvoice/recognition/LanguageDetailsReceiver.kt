package io.github.stream29.dashvoice.recognition

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import java.util.Locale

class LanguageDetailsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS) return

        resultCode = Activity.RESULT_OK
        setResultExtras(
            Bundle().apply {
                putString(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    Locale.getDefault().toLanguageTag(),
                )
                putStringArrayList(
                    RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                    supportedLanguageTags,
                )
            },
        )
    }
}
