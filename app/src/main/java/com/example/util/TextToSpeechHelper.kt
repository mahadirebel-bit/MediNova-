package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(
    private val context: Context,
    private val onInitCompleted: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            onInitCompleted(true)
            tts?.language = Locale.US
        } else {
            Log.e("TTS", "Initialization failed")
            onInitCompleted(false)
        }
    }

    fun speak(text: String, isBengali: Boolean) {
        speak(text, if (isBengali) "bn" else "en")
    }

    fun speak(text: String, langCode: String) {
        if (!isInitialized) {
            Log.e("TTS", "TTS not initialized yet")
            return
        }

        stop()

        val locale = when (langCode) {
            "bn" -> Locale("bn", "BD") // Bengali
            "hi" -> Locale("hi", "IN") // Hindi
            "es" -> Locale("es", "ES") // Spanish
            else -> Locale.US
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback
            tts?.language = Locale.US
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MedExplainSpeechId")
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.shutdown()
        isInitialized = false
    }
}
