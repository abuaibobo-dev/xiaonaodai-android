package com.meitu.generator.data.agent

/**
 * 权限感知拦截器 - 工具执行前检查权限
 * 没权限时不调 LLM 报错，而是直接返回用户友好指令
 */
object PermissionInterceptor {
    
    // 各工具需要的 Android 权限
    private val toolPermissions = mapOf(
        "image_generate" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "image_upscale" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "style_transfer" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "background_remove" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "image_understand" to listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
        "image_repair" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "generate_video" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "batch_process" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE),
        "image_filter" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        "share_image" to listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    )
    
    /**
     * 获取工具需要的权限列表
     */
    fun getRequiredPermissions(toolName: String): List<String> {
        return toolPermissions[toolName] ?: emptyList()
    }
    
    /**
     * 检查是否有权限
     * @return null 如果有权限，否则返回友好的提示语
     */
    fun check(context: android.content.Context, toolName: String): String? {
        val permissions = getRequiredPermissions(toolName)
        for (perm in permissions) {
            if (context.checkSelfPermission(perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return when (perm) {
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE ->
                        "需要存储权限才能保存文件，请在弹出的对话框中点击「允许」"
                    else -> "需要权限: $perm，请在设置中授权"
                }
            }
        }
        return null  // 有权限
    }
}
