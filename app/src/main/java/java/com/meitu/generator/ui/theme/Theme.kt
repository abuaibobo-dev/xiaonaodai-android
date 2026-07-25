package com.meitu.generator.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============ 自定义配色数据类 ============
data class AppColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color,
    val accentDisabled: Color,
    val messageUserBg: Color,
    val messageAiBg: Color,
    val messageAiText: Color,
    val systemBg: Color,
    val error: Color,
    val success: Color,
    val warning: Color
)

val LightAppColors = AppColors(
    background = LightColors.Background,
    surface = LightColors.Surface,
    textPrimary = LightColors.TextPrimary,
    textSecondary = LightColors.TextSecondary,
    border = LightColors.Border,
    accent = LightColors.Accent,
    accentDisabled = LightColors.AccentDisabled,
    messageUserBg = LightColors.MessageUserBg,
    messageAiBg = LightColors.MessageAiBg,
    messageAiText = LightColors.MessageAiText,
    systemBg = LightColors.SystemBg,
    error = LightColors.Error,
    success = LightColors.Success,
    warning = LightColors.Warning
)

val DarkAppColors = AppColors(
    background = DarkColors.Background,
    surface = DarkColors.Surface,
    textPrimary = DarkColors.TextPrimary,
    textSecondary = DarkColors.TextSecondary,
    border = DarkColors.Border,
    accent = DarkColors.Accent,
    accentDisabled = DarkColors.AccentDisabled,
    messageUserBg = DarkColors.MessageUserBg,
    messageAiBg = DarkColors.MessageAiBg,
    messageAiText = DarkColors.MessageAiText,
    systemBg = DarkColors.SystemBg,
    error = DarkColors.Error,
    success = DarkColors.Success,
    warning = DarkColors.Warning
)

// ============ CompositionLocal 提供自定义配色 ============
val LocalAppColors = compositionLocalOf { DarkAppColors }

// ============ Material3 亮色 ColorScheme ============
private val LightColorScheme = lightColorScheme(
    primary = LightColors.Accent,
    onPrimary = Color.White,
    secondary = LightColors.TextSecondary,
    onSecondary = Color.White,
    background = LightColors.Background,
    onBackground = LightColors.TextPrimary,
    surface = LightColors.Surface,
    onSurface = LightColors.TextPrimary,
    surfaceVariant = LightColors.Surface,
    onSurfaceVariant = LightColors.TextSecondary,
    outline = LightColors.Border,
    error = LightColors.Error,
    onError = Color.White
)

// ============ Material3 暗色 ColorScheme ============
private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.Accent,
    onPrimary = Color.White,
    secondary = DarkColors.TextSecondary,
    onSecondary = Color.White,
    background = DarkColors.Background,
    onBackground = DarkColors.TextPrimary,
    surface = DarkColors.Surface,
    onSurface = DarkColors.TextPrimary,
    surfaceVariant = DarkColors.Surface,
    onSurfaceVariant = DarkColors.TextSecondary,
    outline = DarkColors.Border,
    error = DarkColors.Error,
    onError = Color.White
)

// ============ 暗色模式标记 ============
val LocalIsDarkTheme = compositionLocalOf { true }

@Composable
fun MeituTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) DarkColors.Background else LightColors.Background
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// ============ 便捷访问扩展 ============
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current

    val isDark: Boolean
        @Composable get() = LocalIsDarkTheme.current
}
