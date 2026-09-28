package ai.multiwork.companion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, OverlayReminderService::class.java).apply {
            putExtra("title", intent.getStringExtra("title") ?: "MULTIWORK reminder")
            putExtra("message", intent.getStringExtra("message") ?: "You have a reminder.")
        }
        ContextCompat.startForegroundService(context, service)
    }
}