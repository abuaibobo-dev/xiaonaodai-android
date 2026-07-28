package com.meitu.generator.data.tools

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.model.ToolContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 项目文件读取工具
 * 支持列出目录和读取文件内容，用于为代码生成提供上下文
 */
@Singleton
class ProjectFileTool @Inject constructor(
    @ApplicationContext private val appContext: Context
) : Tool {
    override val name = "project_file"
    override val description = "读取项目源代码文件，支持列出目录和读取文件内容"
    override val parametersSchema = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply {
            add("action", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "操作类型：listFiles（列出目录）或 readFile（读取文件）")
                add("enum", JsonArray().apply {
                    add("listFiles")
                    add("readFile")
                })
            })
            add("directory", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "目录路径（listFiles 时使用，默认为项目根目录）")
            })
            add("filePath", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "文件路径（readFile 时使用，相对于项目根目录）")
            })
        })
        add("required", JsonArray().apply {
            add("action")
        })
    }

    companion object {
        private val ALLOWED_EXTENSIONS = setOf(
            "kt", "java", "xml", "gradle", "json", "properties", "md",
            "kts", "toml", "yml", "yaml", "txt", "cfg", "conf"
        )
        private const val MAX_LINES = 500
        private const val PREVIEW_LINES = 200
    }

    private fun getProjectRoot(): File {
        return File(appContext.filesDir, "project")
    }

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val action = arguments["action"] as? String
            ?: return "错误: 缺少 action 参数"

        val projectRoot = getProjectRoot()
        if (!projectRoot.exists()) {
            return "错误: 项目目录不存在 (${projectRoot.absolutePath})。请先创建项目。"
        }

        return when (action) {
            "listFiles" -> listFiles(projectRoot, arguments["directory"] as? String)
            "readFile" -> readFile(projectRoot, arguments["filePath"] as? String)
            else -> "错误: 未知的 action 类型 '$action'，支持: listFiles, readFile"
        }
    }

    private fun listFiles(projectRoot: File, directory: String?): String {
        val targetDir = if (directory.isNullOrBlank()) {
            projectRoot
        } else {
            val dir = File(projectRoot, directory)
            // 安全检查：确保路径在项目目录内
            if (!dir.canonicalPath.startsWith(projectRoot.canonicalPath)) {
                return "错误: 不允许访问项目目录外的路径"
            }
            dir
        }

        if (!targetDir.exists()) {
            return "错误: 目录不存在: ${targetDir.absolutePath}"
        }
        if (!targetDir.isDirectory) {
            return "错误: 不是目录: ${targetDir.absolutePath}"
        }

        val entries = targetDir.listFiles()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name }
        ) ?: emptyList()

        if (entries.isEmpty()) {
            return "目录为空: ${targetDir.relativeTo(projectRoot).path.ifBlank { "/" }}"
        }

        val sb = StringBuilder()
        sb.appendLine("📁 目录: ${targetDir.relativeTo(projectRoot).path.ifBlank { "/" }}")
        sb.appendLine()
        for (entry in entries) {
            val prefix = if (entry.isDirectory) "📁" else "📄"
            sb.appendLine("  $prefix ${entry.name}")
        }
        sb.appendLine()
        sb.appendLine("共 ${entries.size} 项")
        return sb.toString()
    }

    private fun readFile(projectRoot: File, filePath: String?): String {
        if (filePath.isNullOrBlank()) {
            return "错误: 缺少 filePath 参数"
        }

        val file = File(projectRoot, filePath)

        // 安全检查：确保路径在项目目录内
        if (!file.canonicalPath.startsWith(projectRoot.canonicalPath)) {
            return "错误: 不允许访问项目目录外的路径"
        }

        if (!file.exists()) {
            return "错误: 文件不存在: $filePath"
        }
        if (!file.isFile) {
            return "错误: 不是文件: $filePath"
        }

        // 检查文件扩展名
        val ext = file.extension.lowercase()
        if (ext !in ALLOWED_EXTENSIONS) {
            return "错误: 不允许读取 .$ext 文件。支持的文件类型: ${ALLOWED_EXTENSIONS.joinToString(", ")}"
        }

        // 检查文件大小（最大 1MB）
        if (file.length() > 1_000_000) {
            return "错误: 文件过大 (${file.length() / 1024}KB)，最大支持 1MB"
        }

        return try {
            val lines = file.readLines()
            val totalLines = lines.size

            if (totalLines > MAX_LINES) {
                val sb = StringBuilder()
                sb.appendLine("⚠️ 文件较大 ($totalLines 行)，仅显示前 $PREVIEW_LINES 行:")
                sb.appendLine("文件: $filePath")
                sb.appendLine()
                for (i in 0 until PREVIEW_LINES) {
                    sb.appendLine("${String.format("%4d", i + 1)} | ${lines[i]}")
                }
                sb.appendLine()
                sb.appendLine("... (省略 ${totalLines - PREVIEW_LINES} 行，共 $totalLines 行)")
                sb.toString()
            } else {
                val sb = StringBuilder()
                sb.appendLine("文件: $filePath ($totalLines 行)")
                sb.appendLine()
                for (i in lines.indices) {
                    sb.appendLine("${String.format("%4d", i + 1)} | ${lines[i]}")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            "错误: 读取文件失败 - ${e.message?.take(200)}"
        }
    }
}
