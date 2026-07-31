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

// ============ 暗色模式配色 - 深色科技风玻璃拟态 ============
// 参考图风格：纯黑哑光底，高透毛玻璃卡片，极细浅灰高亮边框，柔和底部投影
object DarkColors {
    val Background = Color(0xFF000000)       // 纯黑哑光背景（参考图风格）
    val Surface = Color(0x99FFFFFF)          // 高透毛玻璃白底
    val SurfaceAlpha = Color(0x4DFFFFFF)     // 更透明的玻璃
    val TextPrimary = Color(0xFFE8E8E8)      // 浅灰白（参考图风格）
    val TextSecondary = Color(0xFF999999)    // 中灰
    val Border = Color(0x33FFFFFF)           // 极细浅灰边框（参考图风格）
    val Accent = Color(0xFF7C5CFC)           // 蓝紫强调色（保留品牌色）
    val AccentDisabled = Color(0xFF4A3A8C)
    val MessageUserBg = Color(0xCC7C5CFC)    // 半透明蓝紫用户气泡
    val MessageAiBg = Color(0x19FFFFFF)      // 超高透玻璃AI气泡
    val MessageAiText = Color(0xFFE8E8E8)    // 浅灰白文字
    val MessageUserText = Color(0xFFFFFFFF)
    val SystemBg = Color(0x1AFFFFFF)
    val Error = Color(0xFFFF453A)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)
    val Online = Color(0xFF30D158)
    val AccentAlpha12 = Color(0x1F7C5CFC)
    val TextTertiary = Color(0xFF666666)
    val CardShadow = Color(0x40000000)       // 卡片底部阴影色
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

// ============ 玻璃拟态辅助色 ============
object GlassColors {
    val BorderGlow = Color(0x4D7C5CFC)       // 蓝紫发光（仅用于气泡高亮）
    val InnerGlow = Color(0x1A7C5CFC)        // 内部光晕
    val HighlightBorder = Color(0x33FFFFFF)  // 高亮边框（参考图风格）
    val GradientStart = Color(0xFF000000)    // 纯黑渐变起点
    val GradientEnd = Color(0xFF050505)      // 微差渐变终点
    val CardBg = Color(0x1AFFFFFF)           // 卡片背景（高透）
    val GlassCardBg = Color(0x0DFFFFFF)      // 超透玻璃卡片
    val Overlay = Color(0x80000000)           // 遮罩
    // 按钮颜色
    val ButtonBg = Color(0x1AFFFFFF)         // 半透明白按钮背景
    val ButtonBorder = Color(0x33FFFFFF)     // 按钮边框
    val SendButtonBg = Color(0xCC7C5CFC)     // 发送按钮蓝紫底
    val AccentGreen = Color(0xFF30D158)      // 绿色强调（参考图风格）
}