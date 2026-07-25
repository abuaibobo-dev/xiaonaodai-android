package com.meitu.generator.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.entity.TaskEntity
import com.meitu.generator.repository.GenerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    genRepo: GenerationRepository
) : ViewModel() {
    val tasks: StateFlow<List<TaskEntity>> = genRepo.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
