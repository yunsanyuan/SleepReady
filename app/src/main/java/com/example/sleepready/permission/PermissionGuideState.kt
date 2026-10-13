package com.example.sleepready.permission

import android.content.Context
import android.content.SharedPreferences

object PermissionGuideState {

    private const val PREFS_NAME = "permission_guide_state"
    private const val KEY_PREFIX_SKIP = "skipped_"
    private const val KEY_DEGRADED_TRIGGERED = "degraded_reminder_triggered"
    private const val COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun markSkipped(context: Context, key: String) {
        prefs(context).edit()
            .putLong(KEY_PREFIX_SKIP + key, System.currentTimeMillis())
            .apply()
    }

    fun isInCooldown(context: Context, key: String): Boolean {
        val skippedAt = prefs(context).getLong(KEY_PREFIX_SKIP + key, 0L)
        if (skippedAt == 0L) return false
        return System.currentTimeMillis() - skippedAt < COOLDOWN_MS
    }

    fun markDegradedReminderTriggered(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_DEGRADED_TRIGGERED, true)
            .apply()
    }

    fun consumeDegradedReminderFlag(context: Context): Boolean {
        val prefs = prefs(context)
        val was = prefs.getBoolean(KEY_DEGRADED_TRIGGERED, false)
        if (was) prefs.edit().remove(KEY_DEGRADED_TRIGGERED).apply()
        return was
    }
}