package com.example.sleepready.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem
import com.example.sleepready.domain.model.KnowledgeStatus
import com.example.sleepready.repository.KnowledgeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 顶部筛选：全部 / 单个分类。 */
sealed interface InboxFilter {
    data object All : InboxFilter
    data class ByCategory(val category: KnowledgeCategory) : InboxFilter
}

sealed interface InboxUiEvent {
    data object RequireTitle : InboxUiEvent
    data object RequireCategory : InboxUiEvent
    data object PendingPoolEmpty : InboxUiEvent
    data class RandomPicked(val item: KnowledgeItem) : InboxUiEvent
}

class KnowledgeInboxViewModel(
    private val repo: KnowledgeRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<InboxFilter>(InboxFilter.All)
    val filter: StateFlow<InboxFilter> = _filter.asStateFlow()

    private val _events = MutableSharedFlow<InboxUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<InboxUiEvent> = _events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<KnowledgeItem>> = _filter
        .flatMapLatest { f ->
            when (f) {
                InboxFilter.All -> repo.observeAll()
                is InboxFilter.ByCategory -> repo.observeByCategory(f.category)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: InboxFilter) {
        _filter.value = filter
    }

    fun submit(
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        imageUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _events.tryEmit(InboxUiEvent.RequireTitle)
                return@launch
            }
            if (categories.isEmpty()) {
                _events.tryEmit(InboxUiEvent.RequireCategory)
                return@launch
            }
            repo.add(title, categories, link, note, imageUris)
        }
    }

    fun setStatus(item: KnowledgeItem, status: KnowledgeStatus) {
        viewModelScope.launch { repo.setStatus(item.id, status) }
    }

    fun setStatusById(id: Long, status: KnowledgeStatus) {
        viewModelScope.launch { repo.setStatus(id, status) }
    }

    /** 左滑：直接设为「已抵达」（DONE）。 */
    fun markDone(item: KnowledgeItem) {
        setStatus(item, KnowledgeStatus.DONE)
    }

    fun pickRandom() {
        viewModelScope.launch {
            val picked = repo.pickRandomActive()
            if (picked == null) _events.tryEmit(InboxUiEvent.PendingPoolEmpty)
            else _events.tryEmit(InboxUiEvent.RandomPicked(picked))
        }
    }

    fun markStudying(id: Long) {
        viewModelScope.launch {
            // "去学习" 视为切换到「在路上」
            repo.setStatus(id, KnowledgeStatus.PAUSED)
        }
    }

    fun updateItem(
        id: Long,
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        keepImagePaths: List<String> = emptyList(),
        newImageUris: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            val result = repo.updateItem(id, title, categories, link, note, keepImagePaths, newImageUris)
            if (result.isFailure) _events.tryEmit(InboxUiEvent.RequireTitle)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}
