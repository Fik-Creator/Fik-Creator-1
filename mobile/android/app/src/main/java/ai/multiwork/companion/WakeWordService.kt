package ai.multiwork.companion

import android.app.*
import android.content.Intent
import android.os.*
import android.speech.*
import androidx.core.app.NotificationCompat

class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private val channel = "multiwork_assistant"

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "MULTIWORK Assistant", NotificationManager.IMPORTANCE_LOW)
            )
        }
        startForeground(701, NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("MULTIWORK assistant is active")
            .setContentText("Listening for “Hi MULTIWORK”")
            .setOngoing(true).build())
        beginListening()
    }

    private fun beginListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { restart() }
            override fun onError(e: Int) { restart() }
            override fun onResults(r: Bundle?) {
                val heard = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (Regex("\\bhi\\s+multiwork\\b", RegexOption.IGNORE_CASE).containsMatchIn(heard)) openApp()
                restart()
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-NG")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        })
    }

    private fun restart() {
        listening = false
        Handler(Looper.getMainLooper()).postDelayed({ if (!isDestroyed) beginListening() }, 700)
    }

    private fun openApp() {
        val i = packageManager.getLaunchIntentForPackage(packageName) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        try { startActivity(i) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        recognizer?.destroy(); recognizer = null; listening = false
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}