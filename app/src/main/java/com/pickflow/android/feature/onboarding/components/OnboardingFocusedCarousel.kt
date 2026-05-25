package com.pickflow.android.feature.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pickflow.android.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private const val CENTER_WIDTH_RATIO = 0.62f
private const val SIDE_SCALE = 0.8f
private val CAROUSEL_SPACING = 12.dp

/** iOS `slideDuration` — 한 사이클에서 unitWidth 만큼 슬라이드하는 시간(초). */
private const val SLIDE_DURATION = 0.8
/** iOS `holdDuration` — 사진이 center에 도달한 뒤 정지하는 시간(초). */
private const val HOLD_DURATION = 2.0
/** 화면 밖 양쪽으로 여유를 두기 위한 사진 반복 횟수. iOS `repeatCount`. */
private const val REPEAT_COUNT = 9

/**
 * iOS `OnboardingFocusedCarousel` 1:1 — center-mode 무한 슬라이드 캐러셀.
 *
 * - 사진들이 좌측으로 등속 슬라이드: `cycleDuration`(= slide 0.8 + hold 2.0초)마다 unitWidth 만큼.
 *   앞 0.8초 동안 한 칸 슬라이드하고, 남은 2.0초는 정지한다.
 * - 각 사진은 정중앙과의 거리에 따라 scale 보간(거리 0 → 1.0, ≥ unitWidth → 0.8).
 * - 한 패턴(imageCount 칸) 진행 후 시간 modulo로 즉시 리셋 → 화면 밖에서만 wrap.
 * - `isAnimating == false`이면 시작 프레임(정지 상태)만 렌더 — 스냅샷/테스트용.
 *
 * 사진은 커스텀 에셋이라 placeholder 박스로 자리만 잡는다.
 */
@Composable
fun OnboardingFocusedCarousel(
    pageId: Int,
    imageCount: Int,
    isAnimating: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (isAnimating && imageCount > 1) {
        AnimatedFocusedCarousel(pageId = pageId, imageCount = imageCount, modifier = modifier)
    } else {
        StaticFocusedCarousel(pageId = pageId, imageCount = imageCount, modifier = modifier)
    }
}

/** 정지 프레임 — 중앙 1장 + (imageCount > 1이면) 양옆 0.8배 2장. iOS `isAnimating=false` 프레임과 동일. */
@Composable
private fun StaticFocusedCarousel(pageId: Int, imageCount: Int, modifier: Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / CENTER_WIDTH_RATIO),
        contentAlignment = Alignment.Center,
    ) {
        val centerWidth = maxWidth * CENTER_WIDTH_RATIO
        val unitWidth = centerWidth + CAROUSEL_SPACING

        if (imageCount > 1) {
            CarouselImage(
                pageId = pageId,
                indexInPattern = 0,
                size = centerWidth,
                modifier = Modifier.offset(x = -unitWidth).scale(SIDE_SCALE),
            )
            CarouselImage(
                pageId = pageId,
                indexInPattern = 2,
                size = centerWidth,
                modifier = Modifier.offset(x = unitWidth).scale(SIDE_SCALE),
            )
        }
        // 중앙(focused) 사진 — 마지막에 그려 최상단.
        CarouselImage(pageId = pageId, indexInPattern = 1, size = centerWidth)
    }
}

/**
 * 무한 슬라이드 — `withFrameNanos` 프레임 시계로 구동. iOS `TimelineView(.animation)` 대응.
 *
 * 경과 시간은 [offset]·[graphicsLayer] 람다(레이아웃·드로우 단계)에서만 읽으므로
 * 매 프레임 갱신해도 recomposition 없이 relayout·redraw만 일어난다.
 */
@Composable
private fun AnimatedFocusedCarousel(pageId: Int, imageCount: Int, modifier: Modifier) {
    val totalImages = imageCount.coerceAtLeast(1)
    val cardCount = totalImages * REPEAT_COUNT
    // 시각 정중앙에 처음 놓일 카드 인덱스. iOS `firstCenterExpandedIndex`.
    val firstCenterIndex = totalImages * (REPEAT_COUNT / 2) + 1

    val elapsedNanos = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> elapsedNanos.longValue = now - startNanos }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / CENTER_WIDTH_RATIO),
        contentAlignment = Alignment.Center,
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val centerWidthPx = containerWidthPx * CENTER_WIDTH_RATIO
        val centerWidthDp = maxWidth * CENTER_WIDTH_RATIO
        val spacingPx = with(LocalDensity.current) { CAROUSEL_SPACING.toPx() }
        val unitWidthPx = centerWidthPx + spacingPx
        val firstCenterBaseX = firstCenterIndex * unitWidthPx + centerWidthPx / 2f
        val initialShift = containerWidthPx / 2f - firstCenterBaseX

        repeat(cardCount) { index ->
            val baseX = index * unitWidthPx + centerWidthPx / 2f
            CarouselImage(
                pageId = pageId,
                indexInPattern = index % totalImages,
                size = centerWidthDp,
                modifier = Modifier
                    .offset {
                        val currentX = baseX + initialShift +
                            slideOffset(elapsedNanos.longValue, totalImages, unitWidthPx)
                        // Box(contentAlignment=Center) 기준 → 중앙에서의 상대 오프셋.
                        IntOffset((currentX - containerWidthPx / 2f).roundToInt(), 0)
                    }
                    .graphicsLayer {
                        val currentX = baseX + initialShift +
                            slideOffset(elapsedNanos.longValue, totalImages, unitWidthPx)
                        val s = focusScale(currentX, containerWidthPx, unitWidthPx)
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}

/**
 * 경과 시간(ns)으로 캐러셀 전체의 가로 이동량(px)을 구한다. iOS 로직 1:1.
 *
 * `cycleDuration`(2.8초)마다 unitWidth 만큼 이동하되, 앞 `SLIDE_DURATION`(0.8초)에 슬라이드하고
 * 남은 `HOLD_DURATION`(2.0초)은 정지. 한 패턴(totalImages 칸) 진행 후 modulo로 즉시 리셋한다 —
 * 반복 사진 덕분에 리셋은 화면 밖에서만 일어나 보이지 않는다.
 */
private fun slideOffset(elapsedNanos: Long, totalImages: Int, unitWidth: Float): Float {
    val elapsed = elapsedNanos / 1_000_000_000.0
    val cycleDuration = SLIDE_DURATION + HOLD_DURATION
    val patternSeconds = cycleDuration * totalImages
    val phaseInPattern = elapsed.mod(patternSeconds)
    val cycleIndex = floor(phaseInPattern / cycleDuration)
    val phaseInCycle = phaseInPattern - cycleIndex * cycleDuration
    // hold 구간에는 slideProgress가 1.0으로 고정 → center 도달 후 정지.
    val slideProgress = (phaseInCycle / SLIDE_DURATION).coerceAtMost(1.0)
    val totalProgress = cycleIndex + slideProgress
    return (-totalProgress * unitWidth).toFloat()
}

/** 정중앙과의 거리로 scale을 보간한다. 거리 0 → 1.0, 거리 ≥ unitWidth → SIDE_SCALE(0.8). */
private fun focusScale(currentX: Float, containerWidth: Float, unitWidth: Float): Float {
    val distFromCenter = abs(currentX - containerWidth / 2f)
    val normalized = (distFromCenter / unitWidth).coerceAtMost(1f)
    return 1f - normalized * (1f - SIDE_SCALE)
}

@Composable
private fun CarouselImage(
    pageId: Int,
    indexInPattern: Int,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val resId = imageResFor(pageId, indexInPattern)
    if (resId != null) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF3A3A3A)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "")
        }
    }
}

@androidx.annotation.DrawableRes
private fun imageResFor(pageId: Int, indexInPattern: Int): Int? = when (pageId) {
    2 -> when (indexInPattern) {
        0 -> R.drawable.onboarding_2_0
        1 -> R.drawable.onboarding_2_1
        2 -> R.drawable.onboarding_2_2
        else -> null
    }
    3 -> when (indexInPattern) {
        0 -> R.drawable.onboarding_3_0
        1 -> R.drawable.onboarding_3_1
        2 -> R.drawable.onboarding_3_2
        else -> null
    }
    else -> null
}
