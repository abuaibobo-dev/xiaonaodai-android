package com.meitu.generator.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局 UI 事件总线 — 用于跨页面通信（如顶栏菜单触发清空对话）
 */
object AppEvents {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun send(event: String) {
        _events.emit(event)
    }
}
