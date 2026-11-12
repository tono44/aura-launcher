package com.aura.launcher.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

object AuraColors {
    val Background = Color(0xFF0E1014)
    val BackgroundLift = Color(0xFF171B22)
    val Card = Color(0xFF1B2028)
    val CardFocused = Color(0xFF262D38)
    val TextPrimary = Color(0xFFE8E6E1)
    val TextSecondary = Color(0xFF8A8F98)
    val Accent = Color(0xFFE8E6E1)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AuraColors.Background,
            surface = AuraColors.Card,
            onSurface = AuraColors.TextPrimary,
            onBackground = AuraColors.TextPrimary,
            primary = AuraColors.Accent,
        ),
        typography = Typography(
            displayLarge = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
            titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal),
            bodyLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal),
            labelMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
        ),
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}
