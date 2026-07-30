package com.meitu.generator.ui.theme

import androidx.compose.ui.graphics.Color

// ============ 亮色模式配色（浅灰底，类似主流IM风格） ============
object LightColors {
    val Background = Color(0xFFF2F2F7)       // 浅灰背景
    val Surface = Color(0xFFFFFFFF)          // 白色卡片/表面
    val TextPrimary = Color(0xFF1C1C1E)      // 深色文字主色
    val TextSecondary = Color(0xFF8E8E93)    // 灰色次要文字
    val Border = Color(0xFFE5E5EA)           // 浅灰边框/分割
    val Accent = Color(0xFF007AFF)           // 蓝色强调色
    val AccentDisabled = Color(0xFFB0C4DE)   // 蓝色禁用态
    val MessageUserBg = Color(0xFF007AFF)    // 用户消息蓝色气泡
    val MessageAiBg = Color(0xFFE9E9EB)      // AI消息浅灰气泡
    val MessageAiText = Color(0xFF1C1C1E)    // AI消息深色文字
    val SystemBg = Color(0xFFE9E9EB)         // 系统消息背景
    val Error = Color(0xFFFF3B30)            // 错误红
    val Success = Color(0xFF34C759)          // 成功绿
    val Warning = Color(0xFFFF9500)          // 警告橙
    val Online = Color(0xFF34C759)           // 在线状态
    val AccentAlpha12 = Color(0x1F007AFF)    // 强调色12%透明
    val TextTertiary = Color(0xFFAEAEB2)     // 三级文字
    val MessageUserText = Color(0xFFFFFFFF)  // 用户消息白色文字
}

// ============ 暗色模式配色（深灰底） ============
object DarkColors {
    val Background = Color(0xFF1C1C1E)       // 深灰背景
    val Surface = Color(0xFF2C2C2E)          // 深灰卡片/表面
    val TextPrimary = Color(0xFFFFFFFF)      // 白色文字主色
    val TextSecondary = Color(0xFF8E8E93)    // 灰色次要文字
    val Border = Color(0xFF3A3A3C)           // 深灰边框/分割
    val Accent = Color(0xFF0A84FF)           // 蓝色强调色
    val AccentDisabled = Color(0xFF4A6FA5)   // 蓝色禁用态
    val MessageUserBg = Color(0xFF0A84FF)    // 用户消息蓝色气泡
    val MessageAiBg = Color(0xFF2C2C2E)      // AI消息深灰气泡
    val MessageAiText = Color(0xFFFFFFFF)    // AI消息白色文字
    val SystemBg = Color(0xFF2C2C2E)         // 系统消息背景
    val Error = Color(0xFFFF453A)            // 错误红
    val Success = Color(0xFF30D158)          // 成功绿
    val Warning = Color(0xFFFF9F0A)          // 警告橙
    val Online = Color(0xFF30D158)           // 在线状态
    val AccentAlpha12 = Color(0x1F0A84FF)    // 强调色12%透明
    val TextTertiary = Color(0xFF636366)     // 三级文字
    val MessageUserText = Color(0xFFFFFFFF)  // 用户消息白色文字
}

// ============ 兼容旧引用的顶层变量 (暗色模式默认值) ============
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
