package com.pickflow.android.feature.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickflow.android.R
import com.pickflow.android.feature.onboarding.components.OnboardingIllustration
import com.pickflow.android.feature.onboarding.components.OnboardingPalette
import com.pickflow.android.feature.onboarding.components.OnboardingPanel
import com.pickflow.android.feature.onboarding.model.OnboardingPageContent
import com.pickflow.android.feature.onboarding.model.defaultOnboardingPages
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 온보딩 컨테이너 — iOS `OnboardingView` 1:1 이식.
 *
 * - 화면은 상단(일러스트)·하단(패널) 두 섹션으로 나뉜다. 하단 패널은 화면의
 *   [PANEL_MIN_HEIGHT_FRACTION]을 **최소 높이**로 잡고 콘텐츠가 그보다 크면 필요한
 *   만큼 더 차지한다. 상단 일러스트가 남은 높이를 전부 받는다. 옛 6:4 고정 분할은
 *   짧은 화면에서 CTA 버튼을 짓눌렀다 — 사유는 [PANEL_MIN_HEIGHT_FRACTION] 참고.
 * - 가로 드래그는 **화면 전체**(상·하단 모두)에서 받지만, 실제로 좌우로
 *   넘어가는(슬라이드되는) 것은 **상단 일러스트 영역뿐**이다. 하단 패널과
 *   PICKFLOW 워드마크는 위치 고정이며 콘텐츠만 현재 페이지에 맞춰 교체된다.
 * - 릴리스 시 드래그 거리 + 속도로 인접 페이지에 스프링 스냅하고, 양 끝
 *   페이지에서는 러버밴딩(0.3배)으로 저항을 준다.
 *
 * 현재 페이지 인덱스는 iOS와 동일하게 ViewModel(`pageIndex`)이 단일 진실원이고,
 * 드래그 중 임시 위치만 화면 로컬 상태(`rowOffsetPx`)로 둔다.
 *
 * 행 오프셋은 일반 `mutableFloatStateOf`로 두고 드래그 콜백에서 **동기적으로**만
 * 갱신한다. 스냅 애니메이션은 드래그 종료 콜백/CTA 탭 코루틴 안에서만 돌아
 * 드래그 입력과 시간상 겹치지 않으므로 둘 사이의 경쟁(race)이 원천 차단된다.
 */
@Preview
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit,
) {
    val completed by viewModel.completed.collectAsStateWithLifecycle()
    val currentIndex by viewModel.pageIndex.collectAsStateWithLifecycle()

    LaunchedEffect(completed) {
        if (completed) onFinished()
    }

    val pages = defaultOnboardingPages
    val scope = rememberCoroutineScope()
    // 상단 일러스트 페이저의 가로 이동량(px). 0 = 0번 페이지, -pageWidth*i = i번 페이지.
    var rowOffsetPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageWidthPx = constraints.maxWidth.toFloat()
        val minOffset = -(pages.size - 1) * pageWidthPx
        val maxOffset = 0f
        // Column 안에서는 ColumnScope 가 수신자라 BoxWithConstraints 의 maxHeight 에
        // 닿지 못한다. 여기서 미리 계산해 둔다.
        val panelMinHeight = maxHeight * PANEL_MIN_HEIGHT_FRACTION

        // 폭이 측정되거나 구성 변경으로 currentIndex가 복원되면 페이저 위치를 맞춘다.
        LaunchedEffect(pageWidthPx) {
            rowOffsetPx = -currentIndex * pageWidthPx
        }

        suspend fun snapTo(target: Int, velocity: Float = 0f) {
            animate(
                initialValue = rowOffsetPx,
                targetValue = -target * pageWidthPx,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) { value, _ -> rowOffsetPx = value }
        }

        val dragState = rememberDraggableState { delta ->
            // 드래그 콜백 — 동기적으로만 갱신(코루틴 없음).
            rowOffsetPx = rubberBanded(rowOffsetPx + delta, minOffset, maxOffset)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OnboardingPalette.panelBackground)
                .testTag("onboarding-screen")
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val target = snapTargetIndex(
                            offset = rowOffsetPx,
                            currentIndex = currentIndex,
                            pageWidth = pageWidthPx,
                            velocity = velocity,
                            pageCount = pages.size,
                        )
                        viewModel.setPage(target)
                        // 릴리스 스냅 — 이 코루틴 안에서만 rowOffsetPx를 움직인다.
                        snapTo(target, velocity)
                    },
                ),
        ) {
            // 상단: 일러스트 페이저 — 이 영역만 좌우로 슬라이드된다.
            // 각 일러스트를 화면 폭(fillMaxSize)으로 두고 index*pageWidth 만큼 가로로
            // 타일링한 뒤 rowOffsetPx로 통째로 민다. Row를 쓰지 않으므로 자식 폭이
            // 레이아웃 제약에 눌릴 여지가 없다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 패널이 필요한 높이를 먼저 가져가고 남은 전부를 일러스트가 받는다.
                    .weight(1f)
                    .clipToBounds(),
            ) {
                pages.forEachIndexed { index, page ->
                    OnboardingIllustration(
                        page = page,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    x = (index * pageWidthPx + rowOffsetPx).roundToInt(),
                                    y = 0,
                                )
                            },
                    )
                }
                // PICKFLOW 워드마크 — 슬라이드와 무관하게 좌상단 고정.
                OnboardingWordmark(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp),
                )
            }

            // 하단: 고정 패널 — 콘텐츠만 currentIndex에 맞춰 교체된다.
            OnboardingPanel(
                page = pages[currentIndex],
                currentIndex = currentIndex,
                pageCount = pages.size,
                onPrimaryTap = {
                    // 마지막 페이지의 "시작하기"만 종료, 그 외 "다음으로"는 한 장 넘긴다.
                    val target = currentIndex + 1
                    viewModel.next()
                    if (target <= pages.lastIndex) scope.launch { snapTo(target) }
                },
                modifier = Modifier.heightIn(min = panelMinHeight),
            )
        }
    }
}

/**
 * 하단 패널이 차지하는 화면 높이의 **최소** 비율.
 *
 * 원래는 상단 6 : 하단 4 weight 고정 분할이었다. Column 의 weight 는 상한이면서
 * 하한이라, 패널 콘텐츠(타이틀 2줄 + 서브타이틀 2줄 + 인디케이터 + 56dp CTA +
 * 패딩 64dp + 간격 56dp ≈ 304dp)가 40% 안에 안 들어가는 순간 마지막 자식인 CTA 가
 * 남은 높이만 받아 **납작하게 눌렸다**(PV-137).
 *
 * 임계점 실측(Robolectric, w411dp):
 *
 * | 화면 높이 | 눌린 CTA 높이 |
 * |---|---|
 * | 760dp+ | 56dp (정상) |
 * | 725dp | 46dp |
 * | 700dp | 36dp |
 * | 640dp | 12dp |
 *
 * 제보 기기(Galaxy S9+, 411x846dp)는 846dp 자체로는 통과하지만 3-버튼 내비바
 * inset 약 48dp 가 패널 콘텐츠에서 빠져나가 임계점 아래로 떨어졌다. fontScale 1.8
 * 에서는 화면 높이와 무관하게 8dp 까지 눌렸다.
 *
 * 그래서 이 값은 **최소치**로만 쓰고 패널은 콘텐츠 높이만큼 커질 수 있게 둔다.
 * 콘텐츠가 40% 안에 들어가는 기기에서는 예전과 똑같이 정확히 40% 로 잡힌다.
 */
private const val PANEL_MIN_HEIGHT_FRACTION = 0.4f

/**
 * 정적 단일 페이지 합성(일러스트 + 패널) — 스냅샷/프리뷰 전용.
 *
 * 실제 화면은 [OnboardingScreen]이 "상단만 슬라이드하는 페이저"로 합성하므로
 * 이 컴포저블은 런타임에 쓰이지 않는다. iOS 스냅샷 테스트의 화면 합성과 1:1로
 * 맞추기 위해 페이지 한 장의 정적 모습만 그린다.
 */
@Composable
fun OnboardingScreenContent(
    page: OnboardingPageContent,
    currentIndex: Int,
    pageCount: Int,
    onPrimaryTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val panelMinHeight = maxHeight * PANEL_MIN_HEIGHT_FRACTION
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OnboardingPalette.panelBackground),
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                OnboardingIllustration(
                    page = page,
                    modifier = Modifier.fillMaxSize(),
                )
                OnboardingWordmark(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp),
                )
            }
            OnboardingPanel(
                page = page,
                currentIndex = currentIndex,
                pageCount = pageCount,
                onPrimaryTap = onPrimaryTap,
                modifier = Modifier.heightIn(min = panelMinHeight),
            )
        }
    }
}

/** iOS `Image(.logo)` 워드마크 — `drawable-xhdpi/logo.png` 사용. */
@Composable
private fun OnboardingWordmark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = "PICKFLOW",
        // 뒤 일러스트는 상태바까지 full-bleed 로 두고 워드마크만 내린다.
        modifier = modifier.statusBarsPadding(),
    )
}

/** 양 끝 페이지를 넘어선 드래그에 저항(0.3배)을 주는 러버밴딩. iOS `edgeRubberBand`. */
private fun rubberBanded(value: Float, min: Float, max: Float): Float {
    val factor = 0.3f
    return when {
        value > max -> max + (value - max) * factor
        value < min -> min + (value - min) * factor
        else -> value
    }
}

/**
 * 릴리스 시점의 페이저 위치와 속도로 스냅할 페이지를 정한다.
 * iOS `makePagerGesture`의 predictedEndTranslation + threshold(pageWidth/4) 대응.
 */
private fun snapTargetIndex(
    offset: Float,
    currentIndex: Int,
    pageWidth: Float,
    velocity: Float,
    pageCount: Int,
): Int {
    if (pageWidth <= 0f) return currentIndex
    val restOffset = -currentIndex * pageWidth
    val dragged = offset - restOffset            // 음수 = 왼쪽(다음 페이지 방향)으로 끈 정도
    val projected = dragged + velocity * 0.12f   // 속도를 더해 살짝 앞을 예측
    val threshold = pageWidth / 4f
    val target = when {
        projected <= -threshold -> currentIndex + 1
        projected >= threshold -> currentIndex - 1
        else -> currentIndex
    }
    return target.coerceIn(0, pageCount - 1)
}
