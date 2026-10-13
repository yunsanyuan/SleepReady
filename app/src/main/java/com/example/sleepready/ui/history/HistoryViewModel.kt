package com.example.sleepready.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.domain.model.ExecutionHistory
import com.example.sleepready.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repo: HistoryRepository
) : ViewModel() {

    val records: StateFlow<List<ExecutionHistory>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }
}
