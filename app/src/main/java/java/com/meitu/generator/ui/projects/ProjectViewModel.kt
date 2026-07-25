package com.meitu.generator.ui.projects

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.dao.TaskDao
import com.meitu.generator.data.local.entity.TaskEntity
import com.meitu.generator.ui.theme.LocalAppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val taskDao: TaskDao
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getStatusText(status: Int): String = when (status) {
        0 -> "进行中"
        1 -> "已完成"
        2 -> "已取消"
        else -> "未知"
    }

    fun getStatusColor(status: Int): Color = when (status) {
        0 -> Color(0xFFCC9933)   // Warning/进行中 - 香槟金近似
        1 -> Color(0xFF339933)   // Success/已完成
        2 -> Color(0xFF999999)   // Tertiary/已取消
        else -> Color(0xFF999999)
    }
}
