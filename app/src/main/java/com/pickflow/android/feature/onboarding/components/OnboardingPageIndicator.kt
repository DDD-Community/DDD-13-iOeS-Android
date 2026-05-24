package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** iOS `OnboardingPageIndicator` 1:1 — 현재 페이지는 20x8 캡슐(주황), 나머지는 8x8 점. */
@Composable
fun OnboardingPageIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == currentIndex
            Row(
                modifier = Modifier
                    .width(if (active) 20.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (active) OnboardingPalette.accentOrange
                        else OnboardingPalette.indicatorInactive,
                    ),
            ) {}
        }
    }
}
