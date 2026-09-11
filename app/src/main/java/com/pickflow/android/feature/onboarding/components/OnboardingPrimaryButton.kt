package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS `OnboardingPrimaryButton` 1:1 — 17sp SemiBold, 주황, 코너 12.
 *
 * 높이 [ONBOARDING_CTA_HEIGHT] 는 디자인상 보장값이다. 짧은 화면·큰 글자에서도 눌리지 않도록
 * 하단 패널이 콘텐츠만큼 늘어난다 — `OnboardingScreen.PANEL_MIN_HEIGHT_FRACTION` 참고.
 */
@Composable
fun OnboardingPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ONBOARDING_CTA_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(OnboardingPalette.accentOrange)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnboardingPalette.title,
        )
    }
}

/** 디자인상 보장되어야 하는 온보딩 CTA 높이. 짧은 화면에서도 이 값이 지켜진다. */
val ONBOARDING_CTA_HEIGHT = 58.dp
