package ai.multiwork.companion

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48,72,48,48) }
        root.addView(TextView(this).apply {
            text = "MULTIWORK AI\n\nYour executive operating system."
            textSize = 24f
        })
        root.addView(Button(this).apply {
            text = "Enable “Hi MULTIWORK” assistant"
            setOnClickListener {
                val i = Intent(this@MainActivity, WakeWordService::class.java)
                ContextCompat.startForegroundService(this@MainActivity, i)
            }
        })
        root.addView(TextView(this).apply {
            text = "\nAssistant mode uses the microphone only after you explicitly enable it."
            textSize = 14f
        })
        setContentView(root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }
}