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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() { _toastMessage.value = null }

    fun getStatusText(status: Int): String = when (status) {
        0 -> "进行中"
        1 -> "已完成"
        2 -> "已取消"
        else -> "未知"
    }

    fun getStatusColor(status: Int): Color = when (status) {
        0 -> Color(0xFFCC9933)
        1 -> Color(0xFF339933)
        2 -> Color(0xFF999999)
        else -> Color(0xFF999999)
    }

    fun showNewProjectDialog() {
        _toastMessage.value = "在对话中输入需求，AI 会自动创建项目"
    }
}
