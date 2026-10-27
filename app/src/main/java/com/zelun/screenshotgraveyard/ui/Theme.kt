package com.zelun.screenshotgraveyard.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF111015)
val Surface = Color(0xFF1C1A22)
val Elevated = Color(0xFF25212E)
val Line = Color(0xFF38313F)
val Cream = Color(0xFFF3EEE6)
val Muted = Color(0xFFA49DAE)
val Lavender = Color(0xFFC6B2F6)
val Lime = Color(0xFFD5E8A0)
val Peach = Color(0xFFE7B79E)
val Serif = FontFamily.Serif

@Composable
fun GraveyardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Lavender, onPrimary = Ink, secondary = Lime,
            background = Ink, onBackground = Cream, surface = Surface, onSurface = Cream,
            surfaceVariant = Elevated, onSurfaceVariant = Muted, outline = Line, error = Peach),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = Serif, fontSize = 36.sp, lineHeight = 40.sp, color = Cream),
            headlineMedium = TextStyle(fontFamily = Serif, fontSize = 29.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = Serif, fontSize = 23.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            labelSmall = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium),
        ), content = content,
    )
}
