package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = Color(0xFF002631),
    primaryContainer = Color(0xFF004B5E),
    onPrimaryContainer = Color(0xFFB8F3FF),
    secondary = TealNeon,
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF00513F),
    onSecondaryContainer = Color(0xFF70F7D1),
    tertiary = BlueArm,
    background = ObsidianBg,
    onBackground = SlateText,
    surface = ObsidianSurface,
    onSurface = SlateText,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = SlateMuted,
    error = Color(0xFFFF5252)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00677F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF006B54),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF83F8D3),
    onSecondaryContainer = Color(0xFF002018),
    tertiary = BlueArm,
    background = LightBg,
    onBackground = DarkText,
    surface = LightSurface,
    onSurface = DarkText,
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFD32F2F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
