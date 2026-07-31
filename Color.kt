package com.meitu.generator.ui.theme

import androidx.compose.ui.graphics.Color

// ============ 亮色模式配色（保留，主用暗色） ============
object LightColors {
    val Background = Color(0xFFF2F2F7)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8E8E93)
    val Border = Color(0xFFE5E5EA)
    val Accent = Color(0xFF007AFF)
    val AccentDisabled = Color(0xFFB0C4DE)
    val MessageUserBg = Color(0xFF007AFF)
    val MessageAiBg = Color(0xFFE9E9EB)
    val MessageAiText = Color(0xFF1C1C1E)
    val SystemBg = Color(0xFFE9E9EB)
    val Error = Color(0xFFFF3B30)
    val Success = Color(0xFF34C759)
    val Warning = Color(0xFFFF9500)
    val Online = Color(0xFF34C759)
    val AccentAlpha12 = Color(0x1F007AFF)
    val TextTertiary = Color(0xFFAEAEB2)
    val MessageUserText = Color(0xFFFFFFFF)
}

// ============ 暗色模式配色 - iOS 毛玻璃风格 ============
// 玻璃背景：浅蓝紫渐变底，卡片 rgba(255,255,255,0.1) + 边框 rgba(255,255,255,0.2)
object DarkColors {
    val Background = Color(0xFFE8E4F0)       // 浅紫灰底（毛玻璃透出底色）
    val Surface = Color(0x1AFFFFFF)          // 毛玻璃卡片 rgba(255,255,255,0.1)
    val SurfaceAlpha = Color(0x0DFFFFFF)     // 更透的玻璃
    val TextPrimary = Color(0xFF1C1C1E)      // 深灰黑（在玻璃上可读）
    val TextSecondary = Color(0xFF636366)    // 中灰
    val Border = Color(0x33FFFFFF)           // 卡片边框 rgba(255,255,255,0.2)
    val Accent = Color(0xFF7C5CFC)           // 蓝紫强调色
    val AccentDisabled = Color(0xFFB0A8D0)
    val MessageUserBg = Color(0x337C5CFC)    // 半透明紫用户气泡
    val MessageAiBg = Color(0x1AFFFFFF)      // 毛玻璃AI气泡 rgba(255,255,255,0.1)
    val MessageAiText = Color(0xFF1C1C1E)    // 深色文字（玻璃上可读）
    val MessageUserText = Color(0xFF1C1C1E)  // 深色用户文字
    val SystemBg = Color(0x0DFFFFFF)
    val Error = Color(0xFFFF453A)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)
    val Online = Color(0xFF30D158)
    val AccentAlpha12 = Color(0x1F7C5CFC)
    val TextTertiary = Color(0xFF8E8E93)
    val CardShadow = Color(0x1A000000)       // 卡片底部柔和阴影
}

// ============ 兼容旧引用的顶层变量 ============
val BgPrimary = DarkColors.Background
val BgSecondary = DarkColors.Surface
val BgTertiary = DarkColors.SurfaceAlpha
val Divider = DarkColors.Border
val TextPrimary = DarkColors.TextPrimary
val TextSecondary = DarkColors.TextSecondary
val TextTertiary = DarkColors.TextTertiary
val BrandPurple = DarkColors.Accent
val BrandPurpleLight = DarkColors.Accent
val BrandCyan = DarkColors.Accent
val SuccessGreen = DarkColors.Success
val ErrorRed = DarkColors.Error
val WarningOrange = DarkColors.Warning
val UploadBlue = DarkColors.Accent

// ============ 玻璃拟态辅助色（iOS 毛玻璃风格） ============
object GlassColors {
    val BorderGlow = Color(0x33FFFFFF)       // 卡片高亮边框 rgba(255,255,255,0.2)
    val InnerGlow = Color(0x0DFFFFFF)        // 内部光晕
    val HighlightBorder = Color(0x33FFFFFF)  // 边框
    val GradientStart = Color(0xFFD8D4E8)    // 渐变起点（浅紫）
    val GradientEnd = Color(0xFFE8E4F0)      // 渐变终点（浅灰紫）
    val CardBg = Color(0x1AFFFFFF)           // 卡片背景 rgba(255,255,255,0.1)
    val GlassCardBg = Color(0x0DFFFFFF)      // 超透玻璃
    val Overlay = Color(0x1A000000)           // 遮罩
    // 按钮颜色
    val ButtonBg = Color(0x1AFFFFFF)         // 半透明白按钮
    val ButtonBorder = Color(0x33FFFFFF)     // 按钮边框
    val SendButtonBg = Color(0xCC7C5CFC)     // 发送按钮
    val AccentGreen = Color(0xFF30D158)
}