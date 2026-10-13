package com.example.sleepready.ui.timer

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.data.prefs.PreferencesRepository
import com.example.sleepready.domain.model.TimerConfig
import com.example.sleepready.domain.model.TimerPreset
import com.example.sleepready.repository.TimerConfigRepository
import com.example.sleepready.repository.TimerPresetRepository
import com.example.sleepready.service.TimerState
import com.example.sleepready.service.TimerStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimerPresetListEvent {
    data class StartFailed(val message: String) : TimerPresetListEvent
    data object NavigateToCountdown : TimerPresetListEvent
}

class TimerPresetListViewModel(
    application: Application,
    private val repo: TimerPresetRepository,
    private val timerConfigRepo: TimerConfigRepository,
    private val prefs: PreferencesRepository
) : AndroidViewModel(application) {

    val items: StateFlow<List<TimerPreset>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val runningPresetId: StateFlow<Long> = prefs.observeRunningPresetId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.runningPresetId)

    val timerState: StateFlow<TimerState> = TimerStateHolder.state

    private val _onboardingNeeded = MutableStateFlow(!prefs.onboardingShown)
    val onboardingNeeded: StateFlow<Boolean> = _onboardingNeeded

    fun dismissOnboarding() {
        prefs.onboardingShown = true
        _onboardingNeeded.value = false
    }

    private val _events = MutableSharedFlow<TimerPresetListEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<TimerPresetListEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            TimerStateHolder.state.collect { state ->
                when (state) {
                    TimerState.Idle, is TimerState.Finished -> {
                        if (prefs.runningPresetId != 0L) prefs.runningPresetId = 0L
                    }
                    else -> {}
                }
            }
        }
    }

    fun toggle(preset: TimerPreset) {
        viewModelScope.launch {
            try {
                val running = prefs.runningPresetId
                if (running == preset.id) {
                    cancelInternal()
                    return@launch
                }
                if (running != 0L) {
                    cancelInternal()
                    waitForIdle(timeoutMs = 1500L)
                }
                timerConfigRepo.save(TimerConfig(preset.durationMinutes, preset.plan))
                prefs.lastTimerDurationMinutes = preset.durationMinutes
                repo.touchLastUsed(preset.id, System.currentTimeMillis())
                prefs.runningPresetId = preset.id

                val ctx = getApplication<Application>()
                val serviceClass = Class.forName(SERVICE_CLASS_NAME)
                val intent = Intent(ctx, serviceClass).apply { action = ACTION_START }
                ContextCompat.startForegroundService(ctx, intent)

                _events.tryEmit(TimerPresetListEvent.NavigateToCountdown)
            } catch (t: Throwable) {
                _events.tryEmit(
                    TimerPresetListEvent.StartFailed(t.message ?: t::class.simpleName.orEmpty())
                )
            }
        }
    }

    private suspend fun waitForIdle(timeoutMs: Long) {
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            TimerStateHolder.state.first { it is TimerState.Idle }
        }
    }

    private fun cancelInternal() {
        runCatching {
            val ctx = getApplication<Application>()
            val serviceClass = Class.forName(SERVICE_CLASS_NAME)
            val intent = Intent(ctx, serviceClass).apply { action = ACTION_CANCEL }
            ContextCompat.startForegroundService(ctx, intent)
        }
        prefs.runningPresetId = 0L
        TimerStateHolder.reset()
    }

    fun add(preset: TimerPreset) {
        viewModelScope.launch { repo.add(preset) }
    }

    fun update(preset: TimerPreset) {
        viewModelScope.launch { repo.update(preset) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            if (prefs.runningPresetId == id) cancelInternal()
            repo.delete(id)
        }
    }

    fun togglePin(id: Long, pinned: Boolean) {
        viewModelScope.launch { repo.setPinned(id, pinned) }
    }

    fun moveUp(id: Long) {
        viewModelScope.launch { repo.moveUp(id) }
    }

    fun moveDown(id: Long) {
        viewModelScope.launch { repo.moveDown(id) }
    }

    suspend fun getById(id: Long): TimerPreset? = repo.getById(id)

    companion object {
        const val ACTION_START = "com.example.sleepready.timer.START"
        const val ACTION_CANCEL = "com.example.sleepready.timer.CANCEL"
        private const val SERVICE_CLASS_NAME =
            "com.example.sleepready.service.SleepReadyTimerService"
    }
}
