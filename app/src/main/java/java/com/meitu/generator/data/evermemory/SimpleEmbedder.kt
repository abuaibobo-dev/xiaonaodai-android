package com.meitu.generator.data.evermemory

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 简化版文本向量嵌入器
 *
 * 使用 SHA-256 hash 生成伪向量（64 维 FloatArray），
 * 每个维度由 hash 字节映射到 [-1, 1] 区间并 L2 归一化。
 *
 * 特点：
 * - 确定性：相同文本总是产生相同向量
 * - 分散性：不同文本的向量差异较大（hash 的雪崩效应）
 * - 局限性：无法捕获语义相似性，仅适用于精确匹配和粗粒度聚类
 *
 * 后续可替换为真正的 embedding 模型（如 text-embedding-ada-002）
 */
@Singleton
class SimpleEmbedder @Inject constructor() {

    companion object {
        const val DIMENSION = 64
    }

    /**
     * 将文本转换为 64 维伪向量
     */
    fun embed(text: String): ByteArray {
        val floatVector = embedToFloats(text)
        return floatsToBytes(floatVector)
    }

    /**
     * 将文本转换为 FloatArray 向量（便于计算相似度）
     */
    fun embedToFloats(text: String): FloatArray {
        if (text.isBlank()) return FloatArray(DIMENSION)

        // 使用 SHA-256 生成 hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))

        // 将 hash 字节映射到 64 维浮点向量
        val vector = FloatArray(DIMENSION)
        for (i in 0 until DIMENSION) {
            // 循环使用 hash 字节（SHA-256 = 32 字节，DIMENSION = 64）
            val byteIndex = i % hashBytes.size
            val secondByteIndex = (i + DIMENSION / 2) % hashBytes.size
            // 组合两个字节以获得更好的分布
            val raw = ((hashBytes[byteIndex].toInt() and 0xFF) * 256 +
                    (hashBytes[secondByteIndex].toInt() and 0xFF))
            // 映射到 [-1, 1]
            vector[i] = (raw.toFloat() / 32767.5f) - 1.0f
        }

        // L2 归一化
        return l2Normalize(vector)
    }

    /**
     * 从 ByteArray 还原为 FloatArray 向量
     */
    fun bytesToFloats(bytes: ByteArray): FloatArray {
        val floats = FloatArray(DIMENSION)
        for (i in 0 until DIMENSION) {
            val byteIndex = i * 4
            if (byteIndex + 3 < bytes.size) {
                val bits = ((bytes[byteIndex].toInt() and 0xFF) shl 24) or
                        ((bytes[byteIndex + 1].toInt() and 0xFF) shl 16) or
                        ((bytes[byteIndex + 2].toInt() and 0xFF) shl 8) or
                        (bytes[byteIndex + 3].toInt() and 0xFF)
                floats[i] = Float.fromBits(bits)
            }
        }
        return floats
    }

    /**
     * 计算两个向量的余弦相似度
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }

    /**
     * 计算两个 ByteArray 向量的余弦相似度
     */
    fun cosineSimilarity(a: ByteArray, b: ByteArray): Float {
        return cosineSimilarity(bytesToFloats(a), bytesToFloats(b))
    }

    // ============ 内部工具方法 ============

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (v in vector) norm += v * v
        norm = kotlin.math.sqrt(norm)
        return if (norm > 0f) vector.map { it / norm }.toFloatArray() else vector
    }

    private fun floatsToBytes(floats: FloatArray): ByteArray {
        val bytes = ByteArray(floats.size * 4)
        for (i in floats.indices) {
            val bits = floats[i].toBits()
            bytes[i * 4] = (bits shr 24).toByte()
            bytes[i * 4 + 1] = (bits shr 16).toByte()
            bytes[i * 4 + 2] = (bits shr 8).toByte()
            bytes[i * 4 + 3] = bits.toByte()
        }
        return bytes
    }
}
