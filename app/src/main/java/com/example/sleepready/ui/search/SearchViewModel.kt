package com.example.sleepready.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.search.SearchEngine
import com.example.sleepready.search.SearchableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * 全局搜索 VM。把输入 [query] debounce 200ms 后传给 Java [SearchEngine]，
 * 拿到 [SearchableItem] 列表后按板块分组提供给 UI。
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val engine: SearchEngine
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<SearchableItem>> = _query
        .debounce(200L)
        .distinctUntilChanged()
        .map { q ->
            withContext(Dispatchers.IO) { engine.search(q) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }
}
