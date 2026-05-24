package com.pickflow.android.common.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PickflowColorScheme = darkColorScheme(
    primary = PickflowColors.sunsetOrange,
    onPrimary = PickflowColors.gray0,
    background = PickflowColors.gray95,
    onBackground = PickflowColors.gray0,
    surface = PickflowColors.gray90,
    onSurface = PickflowColors.gray0,
    surfaceVariant = PickflowColors.surfaceChip,
    onSurfaceVariant = PickflowColors.gray30,
    outline = PickflowColors.gray70,
    error = PickflowColors.sunsetOrange,
)

@Composable
fun PickflowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PickflowColorScheme, content = content)
}
