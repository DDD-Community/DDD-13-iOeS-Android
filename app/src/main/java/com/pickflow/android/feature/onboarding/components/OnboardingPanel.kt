package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.onboarding.model.OnboardingPageContent

/**
 * iOS `OnboardingPanel` 1:1 — 타이틀(하이라이트 강조)·서브타이틀·인디케이터·CTA.
 * VStack spacing 28, 내부 타이틀/서브타이틀 spacing 16, padding top 36/bottom 28/h 20.
 */
@Composable
fun OnboardingPanel(
    page: OnboardingPageContent,
    currentIndex: Int,
    pageCount: Int,
    onPrimaryTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OnboardingPalette.panelBackground)
            .padding(top = 36.dp, bottom = 28.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = titleAnnotated(page),
                style = PickflowTypography.headingLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = page.subtitle,
                style = PickflowTypography.bodyMedium,
                color = OnboardingPalette.subtitle,
                textAlign = TextAlign.Center,
            )
        }

        OnboardingPageIndicator(count = pageCount, currentIndex = currentIndex)

        OnboardingPrimaryButton(
            title = if (currentIndex == pageCount - 1) "시작하기" else "다음으로",
            onClick = onPrimaryTap,
        )
    }
}

/** 타이틀에서 [OnboardingPageContent.titleHighlights] 구간만 accent 색으로 칠한다. */
private fun titleAnnotated(page: OnboardingPageContent): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = OnboardingPalette.title)) { append(page.title) }
    page.titleHighlights.forEach { highlight ->
        val start = page.title.indexOf(highlight)
        if (start >= 0) {
            addStyle(
                SpanStyle(color = OnboardingPalette.accentOrange),
                start,
                start + highlight.length,
            )
        }
    }
}
