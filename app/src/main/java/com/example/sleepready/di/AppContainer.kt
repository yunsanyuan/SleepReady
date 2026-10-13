package com.example.sleepready.di

import android.content.Context
import com.example.sleepready.data.db.AppDatabase
import com.example.sleepready.data.prefs.PreferencesRepository
import com.example.sleepready.repository.HistoryRepository
import com.example.sleepready.repository.HistoryRepositoryImpl
import com.example.sleepready.repository.KnowledgeRepository
import com.example.sleepready.repository.KnowledgeRepositoryImpl
import com.example.sleepready.repository.TimerConfigRepository
import com.example.sleepready.repository.TimerConfigRepositoryImpl
import com.example.sleepready.service.ExecutionEngine
import com.example.sleepready.service.ScreenRecordingController
import com.example.sleepready.service.SystemActions
import com.example.sleepready.service.notif.TimerNotifications

/**
 * Hand-rolled DI container. Lives on the [com.example.sleepready.SleepReadyApp] instance
 * and is reachable from any Context via the [appContainer] extension property.
 *
 * The container intentionally constructs nothing eagerly that would touch system services
 * before [SleepReadyApp.onCreate] runs; all dependencies are `lazy`.
 *
 * Wiring detail: [systemActions]'s `onGuidedHint` callback is bound to
 * [TimerNotifications.postGuidedHint] so that whenever a guided
 * settings page is opened (AirplaneMode / WiFi / MobileData / Location / NFC) a
 * matching high-priority notification is posted (Requirement 16.2). The lazy
 * graph is acyclic because [timerNotifications] does not depend on
 * [systemActions].
 */
class AppContainer(private val appContext: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.build(appContext).also {
            com.example.sleepready.repository.DatabaseRef.set(it)
        }
    }
    val prefs: PreferencesRepository by lazy { PreferencesRepository(appContext) }

    val knowledgeRepo: KnowledgeRepository by lazy {
        KnowledgeRepositoryImpl(appContext, database.knowledgeItemDao())
    }
    val historyRepo: HistoryRepository by lazy {
        HistoryRepositoryImpl(database.executionHistoryDao())
    }
    val timerConfigRepo: TimerConfigRepository by lazy {
        TimerConfigRepositoryImpl(database.timerConfigDao(), prefs)
    }
    val timerPresetRepo: com.example.sleepready.repository.TimerPresetRepository by lazy {
        com.example.sleepready.repository.TimerPresetRepositoryImpl(database.timerPresetDao())
    }
    val momentRepo: com.example.sleepready.repository.MomentRepository by lazy {
        com.example.sleepready.repository.MomentRepositoryImpl(appContext, database.momentDao())
    }
    val scheduleRepo: com.example.sleepready.repository.ScheduleRepository by lazy {
        com.example.sleepready.repository.ScheduleRepositoryImpl(database.scheduleDao())
    }

    val macroRepo: com.example.sleepready.repository.MacroRepository by lazy {
        com.example.sleepready.repository.MacroRepositoryImpl(appContext)
    }

    // -- Java 模块（拾句 / 全局搜索 / 数据统计）-----------------------------

    val verseDaoBridge: com.example.sleepready.verse.VerseDaoBridge by lazy {
        com.example.sleepready.verse.VerseDaoBridge(database.verseDao())
    }
    val verseRepo: com.example.sleepready.verse.VerseRepository by lazy {
        com.example.sleepready.verse.VerseRepository(verseDaoBridge)
    }

    val searchDaoBridge: com.example.sleepready.search.SearchDaoBridge by lazy {
        com.example.sleepready.search.SearchDaoBridge(
            scheduleDao = database.scheduleDao(),
            knowledgeDao = database.knowledgeItemDao(),
            momentDao = database.momentDao(),
            verseDao = database.verseDao()
        )
    }
    val searchEngine: com.example.sleepready.search.SearchEngine by lazy {
        com.example.sleepready.search.SearchEngine(searchDaoBridge)
    }

    val statisticsRepo: com.example.sleepready.statistics.StatisticsRepository by lazy {
        com.example.sleepready.statistics.StatisticsRepository(searchDaoBridge)
    }

    val screenRecordingController: ScreenRecordingController by lazy {
        ScreenRecordingController()
    }

    /**
     * Centralised channel registration + notification builders for the timer
     * subsystem. Constructing this triggers `NotificationChannel` registration
     * via its `init` block — safe to do here because `appContext` is already
     * the application context when [AppContainer] is built in
     * [com.example.sleepready.SleepReadyApp.onCreate].
     */
    val timerNotifications: TimerNotifications by lazy {
        TimerNotifications(appContext)
    }

    val systemActions: SystemActions by lazy {
        SystemActions(
            context = appContext,
            screenRecordingController = screenRecordingController,
            // Bind guided-action hints to the notification subsystem (Req 16.2).
            // Method reference is fine: timerNotifications doesn't transitively
            // depend on systemActions, so initialising it here can't recurse.
            onGuidedHint = timerNotifications::postGuidedHint
        )
    }

    val executionEngine: ExecutionEngine by lazy {
        ExecutionEngine(systemActions = systemActions)
    }
}

/**
 * Convenience accessor. Backed by [com.example.sleepready.SleepReadyApp] which
 * exposes the container to every Context (Activity, Service, BroadcastReceiver,
 * AppWidgetProvider, ...).
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as com.example.sleepready.SleepReadyApp).container
