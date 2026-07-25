package com.meitu.generator.util

object ResolutionHelper {
    data class Resolution(val width: Int, val height: Int)

    fun getResolution(ratio: String, quality: String): Resolution {
        val isHd = quality == "HD"
        return when (ratio) {
            "1:1" -> if (isHd) Resolution(1080, 1080) else Resolution(720, 720)
            "3:4" -> if (isHd) Resolution(1080, 1440) else Resolution(720, 960)
            "9:16" -> if (isHd) Resolution(1080, 1920) else Resolution(720, 1280)
            "16:9" -> if (isHd) Resolution(1920, 1080) else Resolution(1280, 720)
            else -> if (isHd) Resolution(1080, 1080) else Resolution(720, 720)
        }
    }

    fun toSizeString(ratio: String, quality: String): String {
        val r = getResolution(ratio, quality)
        return "${r.width}x${r.height}"
    }
}
