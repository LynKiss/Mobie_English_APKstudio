package com.example.myapplication

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.util.Locale

internal class SpeechController(context: Context) : TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageStatus = engine?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
            isReady = languageStatus >= TextToSpeech.LANG_AVAILABLE
            pendingText?.let { speak(it) }
            pendingText = null
        }
    }

    fun speak(text: String) {
        if (isReady) {
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
        } else {
            pendingText = text
        }
    }

    fun close() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
    }
}

@Composable
internal fun rememberSpeechController(): SpeechController {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember(context) { SpeechController(context) }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }
    return controller
}
