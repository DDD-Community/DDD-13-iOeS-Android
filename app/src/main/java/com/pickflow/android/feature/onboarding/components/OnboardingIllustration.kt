package com.pickflow.android.feature.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTypography
import com.pickflow.android.feature.onboarding.model.OnboardingLayout
import com.pickflow.android.feature.onboarding.model.OnboardingPageContent

/**
 * iOS `OnboardingIllustration` 1:1 — `OnboardingPage.layout`별 분기.
 * 폰 목업/캐러셀 사진은 커스텀 에셋이라 placeholder 박스로 자리만 잡는다.
 */
@Composable
fun OnboardingIllustration(
    page: OnboardingPageContent,
    isCarouselAnimating: Boolean = true,
    modifier: Modifier = Modifier,
    toastText: String? = null,
) {
    val gradient = Brush.verticalGradient(
        colorStops = page.gradientStops.map { it.location to it.color }.toTypedArray(),
    )

    Box(modifier = modifier.clipToBounds()) {
        // backgroundLayer: Step 2/3은 panel base 위에 그라데이션, 그 외는 그라데이션 단독.
        if (page.layout == OnboardingLayout.MOOD_CAROUSEL) {
            Box(Modifier.fillMaxSize().background(OnboardingPalette.panelBackground))
        }
        Box(Modifier.fillMaxSize().background(gradient))

        when (page.layout) {
            OnboardingLayout.TOP_ALIGNED_IMAGE -> Column(Modifier.fillMaxSize()) {
                PhonePlaceholder(
                    Modifier
                        .padding(horizontal = 60.dp)
                        .fillMaxWidth()
                        .aspectRatio(0.52f),
                )
                Spacer(Modifier.weight(1f))
            }

            OnboardingLayout.BOTTOM_ALIGNED_IMAGE -> BoxWithConstraints(Modifier.fillMaxSize()) {
                // 폰 목업은 일러스트 높이의 70%만 차지(하단 정렬) → 상단 30%를 토스트 공간으로 비운다.
                val phoneTop = maxHeight * 0.30f
                // 토스트 — 폰보다 먼저 그려 이미지 '뒤'에 두고, 폰 상단 -90dp에서 위로 올라오게 한다.
                ToastSlot(
                    toastText = toastText,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = phoneTop - 90.dp),
                )
                PhonePlaceholder(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxHeight(0.70f)
                        .aspectRatio(0.52f, matchHeightConstraintsFirst = true),
                )
            }

            OnboardingLayout.MOOD_CAROUSEL -> Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    page.mood?.let { OnboardingMoodHeader(header = it) }
                    OnboardingFocusedCarousel(
                        imageCount = page.carouselImageCount,
                        isAnimating = isCarouselAnimating,
                    )
                }
            }
        }
    }
}

/**
 * Step 1 토스트 슬롯 — 등장 시 아래(이미지 뒤)에서 위로 슬라이드 + 페이드한다.
 * 정지 위치는 호출부 `modifier`가 정한다(폰 상단 -90dp). 이미지보다 먼저 그려 '뒤'에 둔다.
 * iOS `OnboardingToast.offset(y:-90).transition(.offset(y:180) + .opacity)` 1:1.
 */
@Composable
private fun ToastSlot(toastText: String?, modifier: Modifier = Modifier) {
    val slideFromPx = with(LocalDensity.current) { 180.dp.roundToPx() }
    // exit 애니메이션 동안에도 텍스트가 필요하므로 마지막 값을 보존한다.
    var lastText by remember { mutableStateOf("") }
    if (toastText != null) lastText = toastText

    AnimatedVisibility(
        visible = toastText != null,
        modifier = modifier,
        enter = slideInVertically(animationSpec = tween(250)) { slideFromPx } + fadeIn(tween(250)),
        exit = slideOutVertically(animationSpec = tween(250)) { slideFromPx } + fadeOut(tween(250)),
    ) {
        OnboardingToast(text = lastText)
    }
}

/** iOS `Image(page.imageName)` 폰 목업 자리 — 커스텀 에셋이라 placeholder 박스. */
@Composable
private fun PhonePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(36.dp))
            .background(PickflowColors.gray90)
            .border(1.dp, PickflowColors.gray70, RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📱", fontSize = 48.sp)
            Text(
                text = "화면 미리보기",
                style = PickflowTypography.labelMedium,
                color = PickflowColors.gray40,
            )
        }
    }
}
