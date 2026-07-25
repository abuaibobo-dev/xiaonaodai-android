package com.meitu.generator.data.agent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * 主动感知调度器 - WorkManager 定期任务
 * 结合 AgentMemory 中的用户偏好，在特定时间主动推送
 * 轻量规则引擎：判断当前时间 + 上次使用间隔 + 用户偏好
 */
object ProactiveScheduler {
    private const val CHANNEL_ID = "meitu_agent_proactive"
    private const val WORK_NAME_DAILY = "meitu_daily_check"
    private const val WORK_NAME_CLEANUP = "meitu_memory_cleanup"

    fun setupNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI助手提醒",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AI助手主动推送和任务完成通知"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun scheduleDailyCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val dailyWork = PeriodicWorkRequestBuilder<DailyCheckWorker>(
            24, TimeUnit.HOURS
        ).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY, ExistingPeriodicWorkPolicy.KEEP, dailyWork
        )
    }

    fun scheduleMemoryCleanup(context: Context) {
        val cleanupWork = PeriodicWorkRequestBuilder<MemoryCleanupWorker>(
            24, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_CLEANUP, ExistingPeriodicWorkPolicy.KEEP, cleanupWork
        )
    }
}

/**
 * 每日检查 Worker
 */
class DailyCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}

/**
 * 记忆清理 Worker
 */
class MemoryCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}
