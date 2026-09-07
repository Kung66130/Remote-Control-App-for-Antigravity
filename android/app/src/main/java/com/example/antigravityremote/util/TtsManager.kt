package com.example.antigravityremote.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "TtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val thaiLocale = Locale("th", "TH")
    private val englishLocale = Locale.US

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentText.value = ""
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentText.value = ""
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _currentText.value = ""
                }
            })
            Log.d(TAG, "TTS Engine Initialized successfully")
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    fun speak(rawText: String) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        val cleanText = sanitizeTextForSpeech(rawText)
        if (cleanText.isBlank()) return

        stop()

        // Auto-detect language: Thai vs English
        val containsThai = cleanText.any { it in '\u0E00'..'\u0E7F' }
        val targetLocale = if (containsThai) {
            val availability = tts?.isLanguageAvailable(thaiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            Log.d(TAG, "Thai locale availability: $availability")
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                thaiLocale
            } else {
                Log.w(TAG, "Thai not available on device, attempting default locale or English")
                Locale.getDefault()
            }
        } else {
            englishLocale
        }

        val langResult = tts?.setLanguage(targetLocale)
        Log.d(TAG, "setLanguage result: $langResult for $targetLocale")

        tts?.setPitch(1.0f)
        tts?.setSpeechRate(1.0f)

        _currentText.value = cleanText
        _isSpeaking.value = true

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        val speakResult = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        Log.d(TAG, "tts.speak returned: $speakResult (text length: ${cleanText.length}, preview: ${cleanText.take(50)})")
    }

    fun stop() {
        if (tts != null && isInitialized) {
            tts?.stop()
        }
        _isSpeaking.value = false
        _currentText.value = ""
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        fun sanitizeTextForSpeech(input: String): String {
            var text = input

            // 1. Remove multi-line code blocks: ```...```
            text = text.replace(Regex("```[\\s\\S]*?```"), " ")

            // 2. Remove inline code: `code`
            text = text.replace(Regex("`[^`]+`"), " ")

            // 3. Remove Markdown links: [Title](Url) -> Title
            text = text.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

            // 4. Remove bare URLs: https://...
            text = text.replace(Regex("https?://\\S+"), " ")

            // 5. Remove Markdown headers & formatting: #, ##, **, __, ~~
            text = text.replace(Regex("[#*_~>]+"), " ")

            // 6. Clean bullet points & table pipes
            text = text.replace(Regex("^[\\s]*[-*•][\\s]+", RegexOption.MULTILINE), "")
            text = text.replace("|", " ")

            // 7. Collapse repeated whitespace and trim
            return text.replace(Regex("\\s+"), " ").trim()
        }
    }
}
