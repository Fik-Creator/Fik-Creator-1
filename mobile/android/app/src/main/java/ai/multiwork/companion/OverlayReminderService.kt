package ai.multiwork.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
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
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("MULTIWORK reminder")
                .setContentText("A reminder is active.")
                .setOngoing(false)
                .build(),
            if (Build.VERSION.SDK_INT >= 34) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "MULTIWORK reminder"
        val message = intent?.getStringExtra("message") ?: "You have a reminder."
        val actionUrl = intent?.getStringExtra("actionUrl").orEmpty()
        if (Settings.canDrawOverlays(this)) showOverlay(title, message, actionUrl)
        else stopSelf()
        return START_NOT_STICKY
    }

    private fun showOverlay(title: String, message: String, actionUrl: String) {
        if (overlay != null) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 20)
            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 22, 40))
                cornerRadius = 24f
                setStroke(2, Color.rgb(42, 78, 122))
            }
            elevation = 24f
        }

        card.addView(TextView(this).apply {
            text = "MULTIWORK AI"
            textSize = 10f
            setTextColor(Color.rgb(116, 211, 255))
        })

        card.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 0)
        })

        card.addView(TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 10)
        })

        val action = Button(this).apply {
            text = if (actionUrl.isNotBlank()) "Open MULTIWORK" else "Dismiss"
            setOnClickListener {
                if (actionUrl.isNotBlank()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {}
                }
                removeOverlayAndStop()
            }
        }
        card.addView(action)

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 70
        }

        try {
            overlay = card
            windowManager?.addView(card, params)
            Handler(mainLooper).postDelayed({ removeOverlayAndStop() }, 12000)
        } catch (_: Exception) {
            overlay = null
            stopSelf()
        }
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
