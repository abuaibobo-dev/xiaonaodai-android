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
object DarkColors {
    val Background = Color(0xFF0A0A1A)       // 极深蓝黑背景
    val Surface = Color(0xCC1A1A3E)          // 半透明玻璃卡片
    val TextPrimary = Color(0xFFE8E8FF)      // 带蓝调的白色
    val TextSecondary = Color(0xFF8888BB)     // 淡蓝紫灰
    val Border = Color(0xFF2A2A5E)           // 紫蓝边框
    val Accent = Color(0xFF7C5CFC)           // 蓝紫强调色
    val AccentDisabled = Color(0xFF4A3A8C)   // 暗紫禁用态
    val MessageUserBg = Color(0xCC7C5CFC)    // 半透明蓝紫用户气泡
    val MessageAiBg = Color(0xCC1A1A3E)      // 半透明玻璃AI气泡
    val MessageAiText = Color(0xFFE8E8FF)    // 蓝白文字
    val MessageUserText = Color(0xFFFFFFFF)  // 纯白文字
    val SystemBg = Color(0xCC1A1A3E)         // 半透明系统消息
    val Error = Color(0xFFFF453A)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)
    val Online = Color(0xFF30D158)
    val AccentAlpha12 = Color(0x1F7C5CFC)
    val TextTertiary = Color(0xFF55557A)     // 深灰紫
}

// ============ 兼容旧引用的顶层变量 ============
val BgPrimary = DarkColors.Background
val BgSecondary = DarkColors.Surface
val BgTertiary = DarkColors.Surface
val Divider = DarkColors.Border
val TextPrimary = DarkColors.TextPrimary
val TextSecondary = DarkColors.TextSecondary
val TextTertiary = DarkColors.TextSecondary
val BrandPurple = DarkColors.Accent
val BrandPurpleLight = DarkColors.Accent
val BrandCyan = DarkColors.Accent
val SuccessGreen = DarkColors.Success
val ErrorRed = DarkColors.Error
val WarningOrange = DarkColors.Warning
val UploadBlue = DarkColors.Accent

// ============ 玻璃拟态辅助色 ============
object GlassColors {
    val BorderGlow = Color(0x4D7C5CFC)       // 边框发光
    val InnerGlow = Color(0x1A7C5CFC)        // 内部光晕
    val HighlightBorder = Color(0x667C5CFC)  // 高亮边框
    val GradientStart = Color(0xFF0A0A1A)    // 渐变起点
    val GradientEnd = Color(0xFF0F0F2E)      // 渐变终点
    val CardBg = Color(0xE61A1A3E)           // 卡片背景（更不透明）
    val Overlay = Color(0x80000000)           // 遮罩
}
