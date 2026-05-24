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

        OnboardingPrimaryButton(title = "시작하기", onClick = onPrimaryTap)
    }
}

/** iOS `makeTitleAttributedString` 1:1 — 하이라이트 구간만 theme accent 색. */
private fun titleAnnotated(page: OnboardingPageContent): AnnotatedString = buildAnnotatedString {
    val title = page.title
    val highlight = page.titleHighlight
    val start = highlight?.let { title.indexOf(it) } ?: -1
    if (highlight == null || start < 0) {
        withStyle(SpanStyle(color = OnboardingPalette.title)) { append(title) }
        return@buildAnnotatedString
    }
    withStyle(SpanStyle(color = OnboardingPalette.title)) { append(title.substring(0, start)) }
    withStyle(SpanStyle(color = page.theme.accentColor)) {
        append(title.substring(start, start + highlight.length))
    }
    withStyle(SpanStyle(color = OnboardingPalette.title)) {
        append(title.substring(start + highlight.length))
    }
}
