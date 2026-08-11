package io.github.stream29.dashvoice.recognition

import android.os.Bundle
import android.speech.SpeechRecognizer

internal fun recognitionResults(transcript: String): Bundle = Bundle().apply {
    putStringArrayList(
        SpeechRecognizer.RESULTS_RECOGNITION,
        arrayListOf(transcript),
    )
    putFloatArray(
        SpeechRecognizer.CONFIDENCE_SCORES,
        floatArrayOf(-1f),
    )
}

internal fun dashScopeLanguageToBcp47(language: String): String = when (language) {
    "zh" -> "zh-CN"
    "yue" -> "yue-HK"
    "en" -> "en-US"
    "ja" -> "ja-JP"
    "de" -> "de-DE"
    "ko" -> "ko-KR"
    "ru" -> "ru-RU"
    "fr" -> "fr-FR"
    "pt" -> "pt-PT"
    "ar" -> "ar"
    "it" -> "it-IT"
    "es" -> "es-ES"
    "hi" -> "hi-IN"
    "id" -> "id-ID"
    "th" -> "th-TH"
    "tr" -> "tr-TR"
    "uk" -> "uk-UA"
    "vi" -> "vi-VN"
    "cs" -> "cs-CZ"
    "da" -> "da-DK"
    "fil" -> "fil-PH"
    "fi" -> "fi-FI"
    "is" -> "is-IS"
    "ms" -> "ms-MY"
    "no" -> "nb-NO"
    "pl" -> "pl-PL"
    "sv" -> "sv-SE"
    else -> language
}

internal val supportedLanguageTags = arrayListOf(
    "zh-CN",
    "yue-HK",
    "en-US",
    "ja-JP",
    "de-DE",
    "ko-KR",
    "ru-RU",
    "fr-FR",
    "pt-PT",
    "ar",
    "it-IT",
    "es-ES",
    "hi-IN",
    "id-ID",
    "th-TH",
    "tr-TR",
    "uk-UA",
    "vi-VN",
    "cs-CZ",
    "da-DK",
    "fil-PH",
    "fi-FI",
    "is-IS",
    "ms-MY",
    "nb-NO",
    "pl-PL",
    "sv-SE",
)
