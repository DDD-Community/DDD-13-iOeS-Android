package com.pickflow.android.feature.onboarding.components

import androidx.compose.ui.graphics.Color
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.feature.onboarding.model.OnboardingAccentTheme

/** iOS `OnboardingPalette` 1:1 이식. */
object OnboardingPalette {
    val panelBackground = PickflowColors.gray95
    val accentOrange = PickflowColors.sunsetOrange
    val accentBlue = Color(0xFF1E8AF6)
    val title = PickflowColors.gray0
    val subtitle = PickflowColors.gray20
    val indicatorInactive = PickflowColors.gray70
}

/** iOS `OnboardingPage.AccentTheme.accentColor` 1:1. */
val OnboardingAccentTheme.accentColor: Color
    get() = when (this) {
        OnboardingAccentTheme.ORANGE -> OnboardingPalette.accentOrange
        OnboardingAccentTheme.BLUE -> OnboardingPalette.accentBlue
    }
