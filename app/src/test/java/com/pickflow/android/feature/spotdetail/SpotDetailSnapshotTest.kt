package com.pickflow.android.feature.spotdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.spotdetail.components.SpotActionButtons
import com.pickflow.android.feature.spotdetail.components.SpotDetailData
import com.pickflow.android.feature.spotdetail.components.SpotDetailErrorContent
import com.pickflow.android.feature.spotdetail.components.SpotDetailLoadedContent
import com.pickflow.android.feature.spotdetail.components.SpotDetailLoadingContent
import com.pickflow.android.feature.spotdetail.components.SpotDetailNavBar
import com.pickflow.android.feature.spotdetail.components.SpotDetailTheme
import com.pickflow.android.feature.spotdetail.components.SpotHeaderSection
import com.pickflow.android.feature.spotdetail.components.SpotPhotoSection
import com.pickflow.android.feature.spotdetail.components.SpotRealTimeInfoSection
import org.junit.Rule
import org.junit.Test

/**
 * iOS `SpotDetailSnapshotTests` 36케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * iOS는 SpotDetail을 정적 다크 토큰으로 렌더하므로 light/dark가 동일 — Android도
 * 동일 컨텐츠를 양쪽 이름으로 record한다. a11y(accessibilityExtraLarge)는 텍스트가
 * 확대되므로 fontScale 2.0으로 렌더한다.
 */
class SpotDetailSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 852))

    private val defaultSpot = SpotDetailData()
    private val bookmarkedSpot = SpotDetailData(isBookmarked = true)
    private val mineSpot = SpotDetailData(isMine = true, parking = null)
    private val reflectionSpot = SpotDetailData(theme = SpotDetailTheme.Reflection)
    private val noImageSpot = SpotDetailData(hasImage = false)
    private val longCommentSpot = SpotDetailData(
        comment = "걷다 보면 멀리 노을이 번져요.\n하늘 비율을 크게 잡아보세요.\n이 구간은 특히 빛이 아름답습니다.",
    )

    // MARK: - Full screen

    @Test fun screen_loading_light() = screen { SpotDetailLoadingContent() }
    @Test fun screen_loading_dark() = screen { SpotDetailLoadingContent() }
    @Test fun screen_error_light() = screen { SpotDetailErrorContent() }
    @Test fun screen_error_dark() = screen { SpotDetailErrorContent() }
    @Test fun screen_loaded_default_light() = screen { SpotDetailLoadedContent(defaultSpot, false) }
    @Test fun screen_loaded_default_dark() = screen { SpotDetailLoadedContent(defaultSpot, false) }
    @Test fun screen_loaded_bookmarked_light() = screen { SpotDetailLoadedContent(bookmarkedSpot, true) }
    @Test fun screen_loaded_bookmarked_dark() = screen { SpotDetailLoadedContent(bookmarkedSpot, true) }
    @Test fun screen_loaded_mine_light() = screen { SpotDetailLoadedContent(mineSpot, false) }
    @Test fun screen_loaded_mine_dark() = screen { SpotDetailLoadedContent(mineSpot, false) }

    // MARK: - NavBar

    @Test fun navbar_default_light() = component(393, 48) { SpotDetailNavBar() }
    @Test fun navbar_default_dark() = component(393, 48) { SpotDetailNavBar() }
    @Test fun navbar_mine_light() = component(393, 48) { SpotDetailNavBar() }

    // MARK: - Header

    @Test fun header_sunset_light() = component(361, 200) { Padded16 { SpotHeaderSection(defaultSpot) } }
    @Test fun header_sunset_dark() = component(361, 200) { Padded16 { SpotHeaderSection(defaultSpot) } }
    @Test fun header_reflection_light() = component(361, 200) { Padded16 { SpotHeaderSection(reflectionSpot) } }
    @Test fun header_reflection_dark() = component(361, 200) { Padded16 { SpotHeaderSection(reflectionSpot) } }
    @Test fun header_mine_light() = component(361, 200) { Padded16 { SpotHeaderSection(mineSpot) } }
    @Test fun header_mine_dark() = component(361, 200) { Padded16 { SpotHeaderSection(mineSpot) } }
    @Test fun header_long_comment_light() = component(361, 300) { Padded16 { SpotHeaderSection(longCommentSpot) } }
    @Test fun header_a11y_light() =
        component(361, 300, fontScale = 2.0f) { Padded16 { SpotHeaderSection(defaultSpot) } }

    // MARK: - Photo

    @Test fun photo_withImage_light() = component(361, 240) { PaddedH16 { SpotPhotoSection(defaultSpot) } }
    @Test fun photo_withImage_dark() = component(361, 240) { PaddedH16 { SpotPhotoSection(defaultSpot) } }
    @Test fun photo_noImage_light() = component(361, 240) { PaddedH16 { SpotPhotoSection(noImageSpot) } }
    @Test fun photo_noImage_dark() = component(361, 240) { PaddedH16 { SpotPhotoSection(noImageSpot) } }

    // MARK: - Action buttons

    @Test fun action_unbookmarked_light() =
        component(361, 68) { PaddedH16 { SpotActionButtons(isMine = false, isBookmarked = false) } }
    @Test fun action_unbookmarked_dark() =
        component(361, 68) { PaddedH16 { SpotActionButtons(isMine = false, isBookmarked = false) } }
    @Test fun action_bookmarked_light() =
        component(361, 68) { PaddedH16 { SpotActionButtons(isMine = false, isBookmarked = true) } }
    @Test fun action_bookmarked_dark() =
        component(361, 68) { PaddedH16 { SpotActionButtons(isMine = false, isBookmarked = true) } }
    @Test fun action_mine_light() =
        component(361, 64) { PaddedH16 { SpotActionButtons(isMine = true, isBookmarked = false) } }
    @Test fun action_mine_dark() =
        component(361, 64) { PaddedH16 { SpotActionButtons(isMine = true, isBookmarked = false) } }

    // MARK: - RealTime info

    @Test fun realtime_default_light() = component(361, 300) { Padded16 { SpotRealTimeInfoSection(defaultSpot) } }
    @Test fun realtime_default_dark() = component(361, 300) { Padded16 { SpotRealTimeInfoSection(defaultSpot) } }
    @Test fun realtime_mine_light() = component(361, 300) { Padded16 { SpotRealTimeInfoSection(mineSpot) } }
    @Test fun realtime_mine_dark() = component(361, 300) { Padded16 { SpotRealTimeInfoSection(mineSpot) } }
    @Test fun realtime_a11y_light() =
        component(361, 400, fontScale = 2.0f) { Padded16 { SpotRealTimeInfoSection(defaultSpot) } }

    // MARK: - Renderers

    private fun screen(content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(device(393, 852))
        paparazzi.snapshot { PickflowTheme { content() } }
    }

    private fun component(
        wDp: Int,
        hDp: Int,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        paparazzi.unsafeUpdateConfig(device(wDp, hDp, fontScale))
        paparazzi.snapshot {
            PickflowTheme {
                // iOS swift-snapshot-testing의 `.fixed` 레이아웃은 컨텐츠를 자연 높이로
                // 측정해 프레임 중앙에 배치한다(오버플로 시 상하 균등 클립).
                // wrapContentHeight(unbounded=true)로 동일하게 무제약 측정 + 중앙 정렬.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PickflowColors.gray95),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.wrapContentHeight(unbounded = true)) {
                        content()
                    }
                }
            }
        }
    }

    private companion object {
        fun device(wDp: Int, hDp: Int, fontScale: Float = 1f): DeviceConfig =
            DeviceConfig.PIXEL_5.copy(
                screenWidth = wDp * 2,
                screenHeight = hDp * 2,
                xdpi = 320,
                ydpi = 320,
                density = Density.XHIGH,
                fontScale = fontScale,
                orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            )
    }
}

/** iOS `headerView`/`realtimeView`의 `.padding(16)` 대응. */
@Composable
private fun Padded16(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(16.dp)) { content() }
}

/** iOS `photoView`/`actionView`의 `.padding(.horizontal, 16)` 대응. */
@Composable
private fun PaddedH16(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) { content() }
}
