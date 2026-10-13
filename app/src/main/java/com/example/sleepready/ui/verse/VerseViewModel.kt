package com.example.sleepready.ui.verse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.domain.model.VerseItem
import com.example.sleepready.verse.VerseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VerseUiEvent {
    data object Saved : VerseUiEvent
    data class RandomPicked(val item: VerseItem) : VerseUiEvent
    data object Empty : VerseUiEvent
    data class Failed(val message: String) : VerseUiEvent
}

/**
 * 拾句板块 ViewModel。所有数据访问都通过 Java 实现的 [VerseRepository]，
 * 在 [Dispatchers.IO] 上调用以避免阻塞主线程。
 */
class VerseViewModel(
    private val repo: VerseRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<VerseItem>>(emptyList())
    val items: StateFlow<List<VerseItem>> = _items.asStateFlow()

    private val _dynasty = MutableStateFlow("全部")
    val dynasty: StateFlow<String> = _dynasty.asStateFlow()

    private val _events = MutableSharedFlow<VerseUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<VerseUiEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun setDynasty(d: String) {
        _dynasty.value = d
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.getByDynasty(_dynasty.value) }
            _items.value = list
        }
    }

    suspend fun getById(id: Long): VerseItem? = withContext(Dispatchers.IO) {
        repo.getById(id)
    }

    fun pickRandom() {
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) { repo.getRandom() }
            if (picked == null) _events.tryEmit(VerseUiEvent.Empty)
            else _events.tryEmit(VerseUiEvent.RandomPicked(picked))
        }
    }

    fun add(title: String, author: String, dynasty: String, content: String, notes: String?) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repo.insert(title, author, dynasty, content, notes) }
                refresh()
                _events.tryEmit(VerseUiEvent.Saved)
            } catch (t: Throwable) {
                _events.tryEmit(VerseUiEvent.Failed(t.message ?: "保存失败"))
            }
        }
    }

    fun update(
        id: Long,
        title: String,
        author: String,
        dynasty: String,
        content: String,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repo.update(id, title, author, dynasty, content, notes) }
                refresh()
                _events.tryEmit(VerseUiEvent.Saved)
            } catch (t: Throwable) {
                _events.tryEmit(VerseUiEvent.Failed(t.message ?: "保存失败"))
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.delete(id) }
            refresh()
        }
    }
}
