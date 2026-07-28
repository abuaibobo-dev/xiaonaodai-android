package com.meitu.generator.data.evermemory

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.max

/**
 * BM25 文本检索器
 *
 * 基于 Okapi BM25 算法实现，是 TF-IDF 的概率改进版本。
 * 核心公式：
 *   score(D, Q) = Σ IDF(qi) × (f(qi,D) × (k1+1)) / (f(qi,D) + k1×(1 - b + b×|D|/avgdl))
 *
 * 参数：
 * - k1: 词频饱和参数（默认 1.5），控制 TF 增长速率
 * - b: 文档长度归一化参数（默认 0.75），0 表示不归一化，1 表示完全归一化
 */
@Singleton
class BM25Retriever @Inject constructor() {

    companion object {
        private const val K1 = 1.5f
        private const val B = 0.75f
    }

    /**
     * 在文档列表中搜索与 query 最相关的文档
     *
     * @param query 查询文本
     * @param documents 候选文档列表
     * @param topK 返回前 K 个结果
     * @return 配对的 (文档内容, 得分) 列表，按得分降序
     */
    fun search(query: String, documents: List<String>, topK: Int = 5): List<Pair<String, Float>> {
        if (documents.isEmpty()) return emptyList()

        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) return emptyList()

        // 对所有文档分词
        val tokenizedDocs = documents.map { tokenize(it) }
        val totalDocs = documents.size
        val avgDocLen = if (totalDocs > 0) tokenizedDocs.map { it.size.toFloat() }.average().toFloat() else 1f

        // 计算每个查询词的 IDF
        val idfMap = queryTerms.associateWith { term ->
            val docFreq = tokenizedDocs.count { docTokens -> docTokens.contains(term) }
            computeIDF(totalDocs, docFreq)
        }

        // 计算每个文档的 BM25 得分
        val scores = tokenizedDocs.mapIndexed { docIndex, docTokens ->
            val docLen = docTokens.size.toFloat()
            val termFreqs = docTokens.groupingBy { it }.eachCount()

            var score = 0f
            for (term in queryTerms) {
                val tf = termFreqs[term]?.toFloat() ?: 0f
                if (tf == 0f) continue
                val idf = idfMap[term] ?: 0f
                val numerator = tf * (K1 + 1)
                val denominator = tf + K1 * (1 - B + B * docLen / avgDocLen)
                score += idf * numerator / denominator
            }
            documents[docIndex] to score
        }

        // 按得分降序排列，返回 topK
        return scores
            .sortedByDescending { it.second }
            .filter { it.second > 0f }
            .take(topK)
    }

    /**
     * 计算 IDF（逆文档频率）
     * 使用经典公式：IDF(t) = ln((N - n + 0.5) / (n + 0.5) + 1)
     */
    private fun computeIDF(totalDocs: Int, docFreq: Int): Float {
        if (docFreq == 0) return 0f
        return ln((totalDocs - docFreq + 0.5f) / (docFreq + 0.5f) + 1f)
    }

    /**
     * 简单分词器
     * - 转小写
     * - 按非字母数字字符分割（支持中文单字切分）
     * - 过滤空 token
     */
    private fun tokenize(text: String): List<String> {
        val normalized = text.lowercase()
        val tokens = mutableListOf<String>()

        // 提取英文单词
        val wordRegex = Regex("[a-z0-9]+")
        tokens.addAll(wordRegex.findAll(normalized).map { it.value })

        // 提取中文字符（单字切分，简化实现）
        val chineseRegex = Regex("[\u4e00-\u9fff]")
        tokens.addAll(chineseRegex.findAll(normalized).map { it.value })

        return tokens
    }
}
