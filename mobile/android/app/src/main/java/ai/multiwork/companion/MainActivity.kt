package ai.multiwork.companion

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 56, 44, 40)
        }
        root.addView(TextView(this).apply { text = "MULTIWORK AI"; textSize = 30f })
        root.addView(TextView(this).apply {
            text = "\nYour executive operating system.\n\nEnable these permissions so MULTIWORK can surface reminders over other apps and keep the “Hi MULTIWORK” assistant available."
            textSize = 15f
        })
        status = TextView(this).apply { textSize = 13f; setPadding(0, 24, 0, 18) }
        root.addView(status)

        root.addView(actionButton("Enable microphone") {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            else startAssistant()
        })
        root.addView(actionButton("Allow display over other apps") {
            if (!Settings.canDrawOverlays(this))
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
            else showOverlayNow()
        })
        root.addView(actionButton("Allow alarms & reminders") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarms = getSystemService(AlarmManager::class.java)
                if (!alarms.canScheduleExactAlarms())
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + packageName)))
                else scheduleTestReminder()
            } else scheduleTestReminder()
        })
        root.addView(actionButton("Enable “Hi MULTIWORK” assistant") { startAssistant() })
        root.addView(actionButton("Test reminder over any app") {
            if (!Settings.canDrawOverlays(this))
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
            else scheduleTestReminder()
        })
        root.addView(actionButton("Open MULTIWORK web workspace") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fik-creator-1.vercel.app/")))
        })
        root.addView(TextView(this).apply {
            text = "\nAndroid controls these capabilities for privacy and battery protection. MULTIWORK only uses them after you enable them."
            textSize = 12f
        })
        setContentView(root)
        requestNotificationPermissionIfNeeded()
        refreshStatus()
    }

    override fun onResume() { super.onResume(); refreshStatus() }

    private fun actionButton(label: String, action: () -> Unit) =
        Button(this).apply { text = label; setOnClickListener { action() } }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
    }

    private fun startAssistant() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
        status.text = "Assistant active — listening for “Hi MULTIWORK”."
    }

    private fun showOverlayNow() {
        val i = Intent(this, OverlayReminderService::class.java).apply {
            putExtra("title", "MULTIWORK is ready")
            putExtra("message", "This is a test of your over-app reminder.")
        }
        ContextCompat.startForegroundService(this, i)
    }

    private fun scheduleTestReminder() {
        val alarmManager = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", "MULTIWORK reminder")
            putExtra("message", "Your executive reminder is ready.")
        }
        val pending = android.app.PendingIntent.getBroadcast(this, 7001, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val at = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }.timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            status.text = "Allow “Alarms & reminders” first, then try again."
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        status.text = "Reminder scheduled for about one minute from now."
    }

    private fun refreshStatus() {
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val overlay = Settings.canDrawOverlays(this)
        val alarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms() else true
        status.text = "Microphone: " + if (mic) "ON" else "OFF" +
            "\nOver other apps: " + if (overlay) "ON" else "OFF" +
            "\nAlarms & reminders: " + if (alarms) "ON" else "OFF"
    }
}