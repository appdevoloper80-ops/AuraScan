package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object TtsHelper {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                } else {
                    Log.e("TtsHelper", "TTS Initialization failed")
                }
            }
        }
    }

    fun speak(text: String, languageName: String) {
        val engine = tts ?: return
        if (!isInitialized) return

        val locale = when (languageName.lowercase()) {
            "hindi" -> Locale("hi", "IN")
            "tamil" -> Locale("ta", "IN")
            "telugu" -> Locale("te", "IN")
            "kannada" -> Locale("kn", "IN")
            "malayalam" -> Locale("ml", "IN")
            "marathi" -> Locale("mr", "IN")
            "bengali" -> Locale("bn", "IN")
            "gujarati" -> Locale("gu", "IN")
            else -> Locale("en", "IN")
        }

        try {
            engine.language = locale
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AuraScan_TTS")
        } catch (e: Exception) {
            Log.e("TtsHelper", "Error speaking text", e)
        }
    }

    fun stop() {
        tts?.stop()
    }
}
