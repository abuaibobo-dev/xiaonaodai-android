package com.meitu.generator.data.agent

import com.meitu.generator.data.agent.AgentMemory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 代码质量评分系统 - 代码生成后自动评分，行级定位问题
 * 
 * 评分维度：
 * 1. 语法完整性 - 括号匹配、语句完整
 * 2. 结构合理性 - 函数长度、嵌套层级、职责单一
 * 3. 命名规范 - 变量/函数命名是否符合规范
 * 4. 注释质量 - 关键逻辑是否有注释
 * 5. 安全性 - 是否有明显安全漏洞（硬编码密码、未处理异常等）
 * 6. 可维护性 - 重复代码、魔术数字、过长参数列表
 * 
 * 评分结果存入记忆，供后续生成时参考避免同类问题
 */
@Singleton
class QualityScoring @Inject constructor(
    private val agentMemory: AgentMemory
) {
    data class QualityScore(
        val overall: Float,           // 总分 0-100
        val syntaxScore: Float,       // 语法完整性
        val structureScore: Float,    // 结构合理性
        val namingScore: Float,       // 命名规范
        val commentScore: Float,      // 注释质量
        val securityScore: Float,     // 安全性
        val maintainabilityScore: Float, // 可维护性
        val issues: List<CodeIssue>   // 发现的问题列表
    )

    data class CodeIssue(
        val line: Int,               // 问题所在行号
        val severity: Severity,      // 严重程度
        val category: String,        // 问题类别
        val description: String,     // 问题描述
        val suggestion: String       // 修复建议
    )

    enum class Severity {
        ERROR,      // 必须修复
        WARNING,    // 建议修复
        INFO        // 优化建议
    }

    /**
     * 对生成的代码进行质量评分
     */
    fun scoreCode(code: String, language: String = "kotlin"): QualityScore {
        val issues = mutableListOf<CodeIssue>()
        val lines = code.lines()

        // 1. 语法完整性检查
        val syntaxScore = checkSyntax(lines, issues)

        // 2. 结构合理性检查
        val structureScore = checkStructure(lines, issues)

        // 3. 命名规范检查
        val namingScore = checkNaming(lines, issues, language)

        // 4. 注释质量检查
        val commentScore = checkComments(lines, issues)

        // 5. 安全性检查
        val securityScore = checkSecurity(lines, issues)

        // 6. 可维护性检查
        val maintainabilityScore = checkMaintainability(lines, issues)

        // 计算总分（加权平均）
        val overall = (
            syntaxScore * 0.25f +
            structureScore * 0.20f +
            namingScore * 0.15f +
            commentScore * 0.10f +
            securityScore * 0.15f +
            maintainabilityScore * 0.15f
        )

        return QualityScore(
            overall = overall,
            syntaxScore = syntaxScore,
            structureScore = structureScore,
            namingScore = namingScore,
            commentScore = commentScore,
            securityScore = securityScore,
            maintainabilityScore = maintainabilityScore,
            issues = issues
        )
    }

    /**
     * 检查语法完整性
     */
    private fun checkSyntax(lines: List<String>, issues: MutableList<CodeIssue>): Float {
        var score = 100f
        var braceCount = 0
        var parenCount = 0
        var bracketCount = 0

        lines.forEachIndexed { index, line ->
            val cleanLine = line.replace("\".*\"".toRegex(), "") // 移除字符串内容
            braceCount += cleanLine.count { it == '{' } - cleanLine.count { it == '}' }
            parenCount += cleanLine.count { it == '(' } - cleanLine.count { it == ')' }
            bracketCount += cleanLine.count { it == '[' } - cleanLine.count { it == ']' }

            // 检查空代码块
            if (Regex("\\{\\s*\\}").containsMatchIn(line) && !line.contains("override")) {
                issues.add(CodeIssue(
                    line = index + 1,
                    severity = Severity.WARNING,
                    category = "syntax",
                    description = "空代码块",
                    suggestion = "添加实现逻辑或注释说明空代码块的用途"
                ))
                score -= 5f
            }
        }

        if (braceCount != 0) {
            issues.add(CodeIssue(
                line = lines.size,
                severity = Severity.ERROR,
                category = "syntax",
                description = "花括号不匹配（差异: $braceCount）",
                suggestion = "检查所有花括号是否正确闭合"
            ))
            score -= 30f
        }
        if (parenCount != 0) {
            issues.add(CodeIssue(
                line = lines.size,
                severity = Severity.ERROR,
                category = "syntax",
                description = "圆括号不匹配（差异: $parenCount）",
                suggestion = "检查所有圆括号是否正确闭合"
            ))
            score -= 30f
        }
        if (bracketCount != 0) {
            issues.add(CodeIssue(
                line = lines.size,
                severity = Severity.ERROR,
                category = "syntax",
                description = "方括号不匹配（差异: $bracketCount）",
                suggestion = "检查所有方括号是否正确闭合"
            ))
            score -= 20f
        }

        return score.coerceAtLeast(0f)
    }

    /**
     * 检查结构合理性
     */
    private fun checkStructure(lines: List<String>, issues: MutableList<CodeIssue>): Float {
        var score = 100f
        var currentFunctionLines = 0
        var maxNesting = 0
        var currentNesting = 0

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // 统计函数长度
            if (trimmed.startsWith("fun ") || trimmed.startsWith("private fun ") || 
                trimmed.startsWith("public fun ") || trimmed.startsWith("suspend fun ")) {
                if (currentFunctionLines > 50) {
                    issues.add(CodeIssue(
                        line = index,
                        severity = Severity.WARNING,
                        category = "structure",
                        description = "函数过长（$currentFunctionLines 行）",
                        suggestion = "考虑将函数拆分为更小的子函数，每个函数不超过 30 行"
                    ))
                    score -= 5f
                }
                currentFunctionLines = 0
            }
            currentFunctionLines++

            // 检查嵌套层级
            currentNesting += line.count { it == '{' } - line.count { it == '}' }
            if (currentNesting > maxNesting) maxNesting = currentNesting
            if (currentNesting > 4) {
                issues.add(CodeIssue(
                    line = index + 1,
                    severity = Severity.WARNING,
                    category = "structure",
                    description = "嵌套层级过深（$currentNesting 层）",
                    suggestion = "考虑使用提前返回、提取函数或设计模式减少嵌套"
                ))
                score -= 3f
            }
        }

        return score.coerceAtLeast(0f)
    }

    /**
     * 检查命名规范
     */
    private fun checkNaming(lines: List<String>, issues: MutableList<CodeIssue>, language: String): Float {
        var score = 100f
        var checked = 0
        var violations = 0

        lines.forEachIndexed { index, line ->
            // 检查变量命名（简单启发式：单字母变量名）
            val singleLetterVars = Regex("(?:val|var|let|const)\\s+([a-z])\\s*[=:]").findAll(line)
            singleLetterVars.forEach { match ->
                val varName = match.groupValues[1]
                if (varName !in listOf("i", "j", "k", "x", "y", "e", "it")) {
                    issues.add(CodeIssue(
                        line = index + 1,
                        severity = Severity.INFO,
                        category = "naming",
                        description = "变量名 '$varName' 过于简短",
                        suggestion = "使用更具描述性的变量名"
                    ))
                    violations++
                }
                checked++
            }

            // 检查函数命名（驼峰式）
            val funcMatch = Regex("fun\\s+([A-Z]\\w*)").find(line)
            if (funcMatch != null) {
                issues.add(CodeIssue(
                    line = index + 1,
                    severity = Severity.WARNING,
                    category = "naming",
                    description = "函数名 '${funcMatch.groupValues[1]}' 应使用小驼峰命名",
                    suggestion = "将首字母改为小写"
                ))
                violations++
                checked++
            }
        }

        if (checked == 0) return 80f // 没有可检查的命名，给基础分
        return (100f * (1f - violations.toFloat() / (checked + 1))).coerceAtLeast(0f)
    }

    /**
     * 检查注释质量
     */
    private fun checkComments(lines: List<String>, issues: MutableList<CodeIssue>): Float {
        var score = 70f // 基础分
        val totalLines = lines.size
        if (totalLines < 5) return 80f // 短代码不强制要求注释

        val commentLines = lines.count { it.trim().startsWith("//") || it.trim().startsWith("/*") || it.trim().startsWith("*") }
        val commentRatio = commentLines.toFloat() / totalLines

        // 注释比例在 10%-30% 之间最佳
        when {
            commentRatio < 0.05f -> {
                issues.add(CodeIssue(
                    line = 1,
                    severity = Severity.INFO,
                    category = "comments",
                    description = "代码注释比例过低（${String.format("%.1f", commentRatio * 100)}%）",
                    suggestion = "为关键逻辑添加注释说明"
                ))
                score -= 20f
            }
            commentRatio > 0.5f -> {
                score -= 10f // 过多注释也可能是问题
            }
            else -> score += 20f
        }

        // 检查是否有 TODO/FIXME
        lines.forEachIndexed { index, line ->
            if (line.contains("TODO") || line.contains("FIXME")) {
                issues.add(CodeIssue(
                    line = index + 1,
                    severity = Severity.INFO,
                    category = "comments",
                    description = "存在待处理项",
                    suggestion = "及时处理或记录到任务跟踪系统"
                ))
            }
        }

        return score.coerceIn(0f, 100f)
    }

    /**
     * 检查安全性
     */
    private fun checkSecurity(lines: List<String>, issues: MutableList<CodeIssue>): Float {
        var score = 100f

        val securityPatterns = listOf(
            Regex("(?i)(password|passwd|secret|api_key|apikey|token)\\s*=\\s*\"[^\"]+\"") to "硬编码敏感信息",
            Regex("TODO|FIXME") to null, // 不算安全问题
            Regex("eval\\s*\\(") to "使用 eval() 可能导致代码注入",
            Regex("Runtime\\.getRuntime\\(\\)\\.exec") to "直接执行系统命令存在安全风险",
            Regex("SELECT.*FROM.*\\+") to "SQL 字符串拼接可能导致注入",
            Regex("catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)\\s*\\{\\s*\\}") to "空异常捕获会隐藏错误"
        )

        lines.forEachIndexed { index, line ->
            securityPatterns.forEach { (pattern, description) ->
                if (description != null && pattern.containsMatchIn(line)) {
                    issues.add(CodeIssue(
                        line = index + 1,
                        severity = Severity.ERROR,
                        category = "security",
                        description = description,
                        suggestion = "使用安全的替代方案，如环境变量、参数化查询等"
                    ))
                    score -= 20f
                }
            }
        }

        return score.coerceAtLeast(0f)
    }

    /**
     * 检查可维护性
     */
    private fun checkMaintainability(lines: List<String>, issues: MutableList<CodeIssue>): Float {
        var score = 100f

        lines.forEachIndexed { index, line ->
            // 检查魔术数字（排除常见数字和索引）
            val magicNumbers = Regex("(?<!=\\s*)(?<![\\w.])([2-9]\\d{1,}|[1-9]\\d{2,})(?![\\w.])").findAll(line)
            magicNumbers.forEach { match ->
                val num = match.groupValues[1]
                if (num !in listOf("100", "1000", "1024", "2048")) {
                    issues.add(CodeIssue(
                        line = index + 1,
                        severity = Severity.INFO,
                        category = "maintainability",
                        description = "魔术数字: $num",
                        suggestion = "提取为有意义的常量"
                    ))
                    score -= 2f
                }
            }

            // 检查过长行
            if (line.length > 120) {
                issues.add(CodeIssue(
                    line = index + 1,
                    severity = Severity.INFO,
                    category = "maintainability",
                    description = "代码行过长（${line.length} 字符）",
                    suggestion = "建议将行控制在 120 字符以内"
                ))
                score -= 1f
            }
        }

        return score.coerceAtLeast(0f)
    }

    /**
     * 将评分结果保存到记忆，供后续参考
     */
    suspend fun recordScore(code: String, score: QualityScore, context: String = "") {
        // 保存最近的评分历史
        val scoreKey = "quality_score_${System.currentTimeMillis()}"
        val scoreValue = buildString {
            appendLine("overall=${score.overall}")
            appendLine("syntax=${score.syntaxScore}")
            appendLine("structure=${score.structureScore}")
            appendLine("naming=${score.namingScore}")
            appendLine("comment=${score.commentScore}")
            appendLine("security=${score.securityScore}")
            appendLine("maintainability=${score.maintainabilityScore}")
            appendLine("issue_count=${score.issues.size}")
            if (context.isNotBlank()) appendLine("context=$context")
            score.issues.filter { it.severity == Severity.ERROR }.take(5).forEach { issue ->
                appendLine("error:L${issue.line}:${issue.description}")
            }
        }
        agentMemory.save(scoreKey, scoreValue, "quality_history")
    }

    /**
     * 获取历史评分趋势摘要
     */
    suspend fun getScoreTrend(): String {
        val history = agentMemory.getByCategory("quality_history")
        if (history.isEmpty()) return "暂无质量评分历史"

        val scores = history.takeLast(10).mapNotNull { entry ->
            val overallLine = entry.value.lines().find { it.startsWith("overall=") }
            overallLine?.substringAfter("=")?.toFloatOrNull()
        }

        if (scores.isEmpty()) return "暂无有效评分数据"

        val avg = scores.average()
        val trend = if (scores.size >= 3) {
            val recent = scores.takeLast(3).average()
            val earlier = scores.dropLast(3).takeLast(3).average()
            when {
                recent > earlier + 5 -> "上升趋势"
                recent < earlier - 5 -> "下降趋势"
                else -> "稳定"
            }
        } else "数据不足"

        return "最近 ${scores.size} 次评分：平均 ${String.format("%.1f", avg)} 分，趋势：$trend"
    }
}
