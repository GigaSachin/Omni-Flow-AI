package com.errorninjas.omniflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = MonsterAmber,
    tertiary = ElectricViolet,
    background = ObsidianDark,
    surface = NeuralBlack,
    onPrimary = Color.Black,
    onBackground = CyberWhite,
    onSurface = CyberWhite
)

@Composable
fun OmniFlowTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
