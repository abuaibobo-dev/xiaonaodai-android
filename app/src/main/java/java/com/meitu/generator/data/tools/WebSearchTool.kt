package com.meitu.generator.data.tools

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.model.ToolContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能搜索工具 - 通过 DuckDuckGo HTML 搜索公开信息
 * 无需 API Key，纯 HTML 解析
 */
@Singleton
class WebSearchTool @Inject constructor() : Tool {
    override val name = "web_search"
    override val description = "联网搜索最新的公开信息，获取实时数据、新闻、价格、技术资料等"

    override val parametersSchema = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply {
            add("query", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "搜索关键词，简洁精准")
            })
        })
        add("required", JsonArray().apply { add("query") })
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val query = arguments["query"] as? String ?: return "错误：缺少搜索关键词"
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://html.duckduckgo.com/html/?q=$encoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return "搜索失败：无响应内容"
            parseResults(html, query)
        } catch (e: Exception) {
            "搜索出错：${e.message?.take(100)}"
        }
    }

    private fun parseResults(html: String, query: String): String {
        val results = mutableListOf<Pair<String, String>>()
        // 提取 result__a 标题和 result__snippet 摘要
        val linkPattern = Regex("""class="result__a"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
        val snippetPattern = Regex("""class="result__snippet"[^>]*>(.*?)</(?:a|span|div)""", RegexOption.DOT_MATCHES_ALL)
        val titles = linkPattern.findAll(html).map { cleanHtml(it.groupValues[1]) }.toList()
        val snippets = snippetPattern.findAll(html).map { cleanHtml(it.groupValues[1]) }.toList()

        for (i in titles.indices) {
            if (i >= 8) break
            val title = titles[i]
            if (title.isBlank()) continue
            val snippet = if (i < snippets.size) snippets[i] else ""
            results.add(title to snippet)
        }

        if (results.isEmpty()) {
            return "未找到关于「$query」的相关结果，请尝试更换关键词"
        }

        return buildString {
            appendLine("🔍 搜索「$query」的结果：")
            appendLine()
            results.forEachIndexed { i, (title, snippet) ->
                appendLine("${i + 1}. $title")
                if (snippet.isNotBlank()) appendLine("   $snippet")
                appendLine()
            }
        }
    }

    private fun cleanHtml(text: String): String {
        return text
            .replace(Regex("""<[^>]*>"""), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&nbsp;", " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
