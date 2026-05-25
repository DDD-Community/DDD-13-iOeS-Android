package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.onboarding.model.OnboardingMood
import com.pickflow.android.feature.onboarding.model.OnboardingMoodKind

/**
 * iOS `OnboardingMoodHeaderView` 1:1 — Step 2/3 일러스트 상단의 mood 헤더.
 * 위: secondary 칩(59x28, opacity 0.5) / 아래: primary 칩(84x40, stroke) / 설명 문구.
 * mood 아이콘은 커스텀 에셋이라 이모지 placeholder.
 */
@Composable
fun OnboardingMoodHeader(
    header: OnboardingMood,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoodChip(
            mood = header.secondary,
            width = 59.dp,
            height = 28.dp,
            isSelected = false,
            modifier = Modifier.alpha(0.5f),
        )
        MoodChip(
            mood = header.primary,
            width = 84.dp,
            height = 40.dp,
            isSelected = true,
        )
        Text(
            text = header.description,
            style = PickflowTypography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun MoodChip(
    mood: OnboardingMoodKind,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val isLarge = height >= 40.dp
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier = modifier
            .width(width)
            .height(height)
            .background(OnboardingPalette.panelBackground, shape)
            .border(1.dp, if (isSelected) mood.borderColor else Color.Transparent, shape),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconResFor(mood)),
            contentDescription = null,
            modifier = Modifier.size(if (isLarge) 18.dp else 14.dp),
        )
        Text(
            text = mood.label,
            style = if (isLarge) PickflowTypography.bodyMediumBold else PickflowTypography.bodySmall,
            color = Color.White,
        )
    }
}

@androidx.annotation.DrawableRes
private fun iconResFor(mood: OnboardingMoodKind): Int = when (mood) {
    OnboardingMoodKind.SUNSET -> R.drawable.ic_sunset
    OnboardingMoodKind.RIPPLE -> R.drawable.ic_reflection
}
