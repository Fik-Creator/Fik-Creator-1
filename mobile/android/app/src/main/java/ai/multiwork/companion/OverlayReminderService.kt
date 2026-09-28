package ai.multiwork.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayReminderService : Service() {
    private var windowManager: WindowManager? = null
    private var overlay: android.view.View? = null
    private val channel = "multiwork_reminders"

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            702,
            NotificationCompat.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("MULTIWORK reminder")
                .setContentText("A reminder is active.")
                .setOngoing(false)
                .build(),
            if (Build.VERSION.SDK_INT >= 34) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0
        )
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "MULTIWORK reminder"
        val message = intent?.getStringExtra("message") ?: "You have a reminder."
        if (Settings.canDrawOverlays(this)) showOverlay(title, message)
        else stopSelf()
        return START_NOT_STICKY
    }

    private fun showOverlay(title: String, message: String) {
        if (overlay != null) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(34, 28, 34, 28)
            setBackgroundColor(Color.rgb(12, 20, 36))
            elevation = 20f
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(Color.WHITE)
        })
        card.addView(TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 0)
        })
        card.setOnClickListener { removeOverlayAndStop() }

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 70
        }

        overlay = card
        windowManager?.addView(card, params)
        android.os.Handler(mainLooper).postDelayed({ removeOverlayAndStop() }, 12000)
    }

    private fun removeOverlayAndStop() {
        overlay?.let { view -> try { windowManager?.removeView(view) } catch (_: Exception) {} }
        overlay = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "MULTIWORK Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun onDestroy() {
        overlay?.let { view -> try { windowManager?.removeView(view) } catch (_: Exception) {} }
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}