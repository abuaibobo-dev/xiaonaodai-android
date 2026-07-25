package com.meitu.generator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    headlineSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
    bodyLarge = TextStyle(fontSize = 15.sp, color = TextSecondary),
    bodyMedium = TextStyle(fontSize = 14.sp, color = TextSecondary),
    labelLarge = TextStyle(fontSize = 13.sp, color = TextSecondary),
    labelMedium = TextStyle(fontSize = 12.sp, color = TextTertiary),
    labelSmall = TextStyle(fontSize = 11.sp, color = TextTertiary)
)
