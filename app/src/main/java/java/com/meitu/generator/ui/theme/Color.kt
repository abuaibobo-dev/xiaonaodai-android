package com.meitu.generator.ui.theme

import androidx.compose.ui.graphics.Color

// ============ 亮色模式配色 ============
object LightColors {
    val Background = Color(0xFFFFFFFF)       // 纯白
    val Surface = Color(0xFFF5F5F5)          // 卡片/表面
    val TextPrimary = Color(0xFF000000)       // 文字主色
    val TextSecondary = Color(0xFF666666)     // 文字次要
    val Border = Color(0xFFE0E0E0)            // 边框/分割
    val Accent = Color(0xFFC9A96E)            // 香槟金强调色
    val AccentDisabled = Color(0xFFD4C5A0)    // 香槟金禁用态
    val MessageUserBg = Color(0xFFFFFFFF)     // 用户消息背景
    val MessageAiBg = Color(0xFF2A2A2A)       // AI消息背景
    val MessageAiText = Color(0xFFFFFFFF)     // AI消息文字
    val SystemBg = Color(0xFFF0F0F0)          // 系统消息背景
    val Error = Color(0xFFCC3333)             // 错误
    val Success = Color(0xFF339933)           // 成功
    val Warning = Color(0xFFCC9933)           // 警告
}

// ============ 暗色模式配色 ============
object DarkColors {
    val Background = Color(0xFF000000)       // 纯黑
    val Surface = Color(0xFF1A1A1A)          // 卡片/表面
    val TextPrimary = Color(0xFFFFFFFF)      // 文字主色
    val TextSecondary = Color(0xFF999999)    // 文字次要
    val Border = Color(0xFF333333)           // 边框/分割
    val Accent = Color(0xFFC9A96E)           // 香槟金强调色
    val AccentDisabled = Color(0xFF7A6B4A)   // 香槟金禁用态
    val MessageUserBg = Color(0xFF2A2A2A)    // 用户消息背景(暗色下)
    val MessageAiBg = Color(0xFF1A1A1A)      // AI消息背景
    val MessageAiText = Color(0xFFFFFFFF)    // AI消息文字
    val SystemBg = Color(0xFF1A1A1A)         // 系统消息背景
    val Error = Color(0xFFFF5555)            // 错误
    val Success = Color(0xFF55CC55)          // 成功
    val Warning = Color(0xFFFFBB44)          // 警告
}

// ============ 兼容旧引用的顶层变量 (暗色模式默认值) ============
// 这些保持不变以确保编译兼容，实际UI应使用 LocalAppColors
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
