package ai.multiwork.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var destroyed = false
    private var commandMode = false
    private val channel = "multiwork_assistant"
    private val handler = Handler(Looper.getMainLooper())
    private val workspaceUrl = "https://multiworkai.vercel.app/"

    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "MULTIWORK Assistant", NotificationManager.IMPORTANCE_LOW)
            )
        }
        updateNotification("Listening for “Hi MULTIWORK”")
        startRecognition(false)
    }

    private fun updateNotification(message: String) {
        startForeground(
            701,
            NotificationCompat.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("MULTIWORK assistant is active")
                .setContentText(message)
                .setOngoing(true)
                .build()
        )
    }

    private fun startRecognition(isCommandMode: Boolean) {
        if (destroyed || !SpeechRecognizer.isRecognitionAvailable(this)) return
        commandMode = isCommandMode
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateNotification(if (commandMode) "Listening for your command" else "Listening for “Hi MULTIWORK”")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (!destroyed) restartWake()
            }
            override fun onResults(results: Bundle?) {
                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()

                if (commandMode) {
                    if (heard.isNotBlank()) {
                        launchWorkspace(heard)
                    }
                    restartWake()
                    return
                }

                val match = Regex("\\bhi\\s+multiwork\\b([\\s,.:;-]*)(.*)$", RegexOption.IGNORE_CASE).find(heard)
                if (match == null) {
                    restartWake()
                    return
                }

                val trailing = match.groupValues.getOrNull(2).orEmpty().trim()
                if (trailing.isNotBlank()) {
                    launchWorkspace(trailing)
                    restartWake()
                } else {
                    showListeningOverlay()
                    handler.postDelayed({
                        if (!destroyed) startRecognition(true)
                    }, 350)
                }
            }
            override fun onPartialResults(params: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        })
    }

    private fun restartWake() {
        if (destroyed) return
        commandMode = false
        handler.postDelayed({ if (!destroyed) startRecognition(false) }, 700)
    }

    private fun launchWorkspace(command: String) {
        val target = workspaceUrl + "?voice=" + Uri.encode(command) + "&source=android_wake"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            showCommandOverlay(command, target)
        }
    }

    private fun showListeningOverlay() {
        val intent = Intent(this, OverlayReminderService::class.java).apply {
            putExtra("title", "MULTIWORK is listening")
            putExtra("message", "Say your command now.")
            putExtra("actionUrl", workspaceUrl + "?voice=__WAKE__&source=android_wake")
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        } catch (_: Exception) {}
    }

    private fun showCommandOverlay(command: String, target: String) {
        val intent = Intent(this, OverlayReminderService::class.java).apply {
            putExtra("title", "MULTIWORK heard you")
            putExtra("message", command)
            putExtra("actionUrl", target)
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
