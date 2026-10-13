package com.example.sleepready

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.sleepready.di.AppContainer
import com.example.sleepready.domain.model.ThemeMode
import com.example.sleepready.service.schedule.AlarmRescheduleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Process-wide [Application] entry point.
 *
 * Responsibilities (Requirements 22.2, 22.3, 22.4, 25.3):
 * - Instantiates the manual DI [AppContainer] in [onCreate] so any Activity / Service /
 *   BroadcastReceiver / AppWidgetProvider can reach dependencies via
 *   [com.example.sleepready.di.appContainer].
 * - Reads the persisted theme preference and applies it via
 *   [AppCompatDelegate.setDefaultNightMode] **before** any Activity inflates content
 *   (Requirement 22.3 — no flash on cold start).
 * - Subscribes to [com.example.sleepready.data.prefs.PreferencesRepository.observeThemeMode]
 *   so changes from Settings take effect immediately without restart (Requirement 22.4 / P14).
 */
class SleepReadyApp : Application() {

    lateinit var container: AppContainer
        private set

    /** Lives for the whole process; theme listener never needs to be cancelled. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)

        // Apply persisted theme synchronously before the first Activity is created.
        AppCompatDelegate.setDefaultNightMode(container.prefs.themeMode.toNightMode())

        // React to live theme changes (e.g. user toggles on the Settings screen).
        container.prefs.observeThemeMode()
            .onEach { AppCompatDelegate.setDefaultNightMode(it.toNightMode()) }
            .launchIn(appScope)

        // Reschedule all schedule alarms on app start (safety net for reboot/update)
        AlarmRescheduleHelper.rescheduleAllAsync(applicationContext)
    }
}

private fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}
