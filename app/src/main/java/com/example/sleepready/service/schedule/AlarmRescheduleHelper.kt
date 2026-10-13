package com.example.sleepready.service.schedule

import android.content.Context
import android.util.Log
import com.example.sleepready.di.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AlarmRescheduleHelper {

    private const val TAG = "AlarmReschedule"
    private const val MAX_RETRIES = 5
    private const val RETRY_DELAY_MS = 2000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun rescheduleAll(context: Context) {
        val repo = context.appContainer.scheduleRepo
        var attempt = 0
        while (attempt < MAX_RETRIES) {
            try {
                val active = repo.getAllActive()
                Log.d(TAG, "rescheduleAll: ${active.size} active schedules")
                active.forEach { schedule ->
                    try {
                        ScheduleReminderScheduler.schedule(context, schedule)
                    } catch (sec: SecurityException) {
                        Log.w(TAG, "SecurityException for ${schedule.id}, skip", sec)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error scheduling ${schedule.id}", t)
                    }
                }
                Log.d(TAG, "rescheduleAll done")
                return
            } catch (t: Throwable) {
                attempt++
                Log.w(TAG, "rescheduleAll attempt $attempt failed", t)
                if (attempt < MAX_RETRIES) delay(RETRY_DELAY_MS)
            }
        }
        Log.e(TAG, "rescheduleAll failed after $MAX_RETRIES attempts")
    }

    fun rescheduleAllAsync(context: Context, onDone: (() -> Unit)? = null) {
        scope.launch {
            rescheduleAll(context)
            onDone?.invoke()
        }
    }
}