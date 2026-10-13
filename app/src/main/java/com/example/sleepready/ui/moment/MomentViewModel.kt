package com.example.sleepready.ui.moment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.domain.model.Moment
import com.example.sleepready.repository.MomentRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MomentUiEvent {
    data object Saved : MomentUiEvent
    data class Failed(val message: String) : MomentUiEvent
}

class MomentViewModel(
    application: Application,
    private val repo: MomentRepository
) : AndroidViewModel(application) {

    val items: StateFlow<List<Moment>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<MomentUiEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<MomentUiEvent> = _events.asSharedFlow()

    fun add(
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            try {
                repo.add(title, body, eventTimeMillis, imageUris)
                _events.tryEmit(MomentUiEvent.Saved)
            } catch (t: Throwable) {
                _events.tryEmit(MomentUiEvent.Failed(t.message ?: "保存失败"))
            }
        }
    }

    fun update(
        id: Long,
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        keepImagePaths: List<String>,
        newImageUris: List<Uri>
    ) {
        viewModelScope.launch {
            try {
                repo.update(id, title, body, eventTimeMillis, keepImagePaths, newImageUris)
                _events.tryEmit(MomentUiEvent.Saved)
            } catch (t: Throwable) {
                _events.tryEmit(MomentUiEvent.Failed(t.message ?: "保存失败"))
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    suspend fun getById(id: Long): Moment? = repo.getById(id)
}
