package com.pickflow.android.feature.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pickflow.android.R
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
import androidx.compose.ui.tooling.preview.Preview
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
                // PNG 자연 비율 유지 — onboarding_0.png는 560x880 (ratio 0.636).
                // aspectRatio 강제 제거, FillWidth로 폭에 맞춰 높이 자동 결정.
                Image(
                    painter = painterResource(R.drawable.onboarding_0),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .padding(horizontal = 55.dp)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
            }

            OnboardingLayout.BOTTOM_ALIGNED_IMAGE -> BoxWithConstraints(Modifier.fillMaxSize()) {
                // onboarding_1.png 자연 비율(0.906) 유지, 하단 정렬.
                val horizontalPaddingDp = 55.dp
                val imageWidth = maxWidth - horizontalPaddingDp * 2
                val imageHeight = imageWidth / 0.906f  // PNG 자연 비율

                // 토스트 — 이미지 top에서 위로 30dp.
                ToastSlot(
                    toastText = toastText,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -(imageHeight + 30.dp)),
                )

                Image(
                    painter = painterResource(R.drawable.onboarding_1),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = horizontalPaddingDp)
                        .fillMaxWidth(),
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
                        pageId = page.id,
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
