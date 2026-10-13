package com.example.sleepready.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Receives the [android.app.AlarmManager.setExactAndAllowWhileIdle] broadcast
 * when the countdown reaches zero (Requirement 13.1) and forwards it to the
 * foreground [SleepReadyTimerService] so the [ExecutionEngine] can run even
 * if the device is in Doze mode (Requirement 13.2).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMEOUT) return
        val forward = Intent(context, SleepReadyTimerService::class.java).apply {
            action = ACTION_TIMEOUT
        }
        ContextCompat.startForegroundService(context, forward)
    }

    companion object {
        const val ACTION_TIMEOUT = "com.example.sleepready.timer.TIMEOUT"
        const val ALARM_REQUEST_CODE = 1001
    }
}
