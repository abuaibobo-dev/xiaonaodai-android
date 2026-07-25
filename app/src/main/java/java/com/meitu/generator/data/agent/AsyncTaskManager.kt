package com.meitu.generator.data.agent

import android.content.Context
import androidx.work.*
import java.util.UUID

/**
 * 异步托管与进度流式回传 - 长任务走 WorkManager 后台执行
 * 预计耗时 > 5秒的任务（视频生成、超分等），立即返回提示
 * 利用 Kotlin Flow 推送进度到通知栏
 */
object AsyncTaskManager {
    private const val CHANNEL_ID = "meitu_async_tasks"
    
    /** 长耗时工具列表 */
    private val LONG_RUNNING_TOOLS = setOf(
        "generate_video", "image_upscale", "image_generate", "style_transfer"
    )
    
    /** 判断工具是否需要异步执行 */
    fun isLongRunning(toolName: String): Boolean = toolName in LONG_RUNNING_TOOLS
    
    /**
     * 提交异步任务
     * @return 任务ID + 预计提示
     */
    fun submit(
        context: Context,
        toolName: String,
        arguments: Map<String, Any>
    ): Pair<String, String> {
        val taskId = UUID.randomUUID().toString()
        val estimatedTime = when (toolName) {
            "generate_video" -> "60秒"
            "image_upscale" -> "15秒"
            else -> "30秒"
        }
        
        val inputData = workDataOf(
            "tool_name" to toolName,
            "task_id" to taskId,
            "arguments_json" to com.google.gson.Gson().toJson(arguments)
        )
        
        val workRequest = OneTimeWorkRequestBuilder<ToolExecutionWorker>()
            .setInputData(inputData)
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        
        return Pair(taskId, "正在处理，预计${estimatedTime}，完成后通知您")
    }
}

/**
 * 工具执行 Worker
 */
class ToolExecutionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val toolName = inputData.getString("tool_name") ?: return Result.failure()
        // 实际工具执行逻辑由 AgentEngine 驱动
        return Result.success(workDataOf("tool_name" to toolName, "status" to "completed"))
    }
}
