package com.example.sleepready.permission

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sleepready.service.schedule.AlarmRingService

object DegradedReminder {

    private const val CHANNEL_ID = "channel_degraded_reminder"
    private const val TAG = "DegradedReminder"

    fun trigger(context: Context, title: String) {
        triggerVibration(context)
        triggerToast(context, title)
        PermissionGuideState.markDegradedReminderTriggered(context)
    }

    fun triggerAlarmRingFallback(context: Context, scheduleId: Long, title: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("印记闹钟：$title")
            .setContentText("时间到了")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND)
            .setVibrate(longArrayOf(0, 600, 400, 600, 400))
            .build()
        try {
            NotificationManagerCompat.from(context)
                .notify((scheduleId % Int.MAX_VALUE).toInt() + 80000, notification)
        } catch (sec: SecurityException) {
            triggerVibration(context)
            triggerToast(context, title)
        }
        PermissionGuideState.markDegradedReminderTriggered(context)
    }

    fun triggerVibration(context: Context) {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, 255))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(500)
            }
        } catch (_: SecurityException) {}
        catch (_: Throwable) {}
    }

    fun triggerToast(context: Context, title: String) {
        try {
            Toast.makeText(context, "您有一个日程提醒：$title", Toast.LENGTH_LONG).show()
        } catch (_: Throwable) {}
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "降级提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "通知权限被拒时的降级闹钟提醒"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(
                null,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(channel)
    }
}