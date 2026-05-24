package com.pickflow.android.feature.spotlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.pickflow.android.feature.spotlist.components.SpotListCell
import com.pickflow.android.feature.spotlist.components.SpotListEmptyContent
import com.pickflow.android.feature.spotlist.components.SpotListFailedContent
import com.pickflow.android.feature.spotlist.components.SpotListGridItem
import com.pickflow.android.feature.spotlist.components.SpotListLoadedGrid
import com.pickflow.android.feature.spotlist.components.SpotListLoadingContent
import com.pickflow.android.feature.spotlist.components.SpotListMood
import com.pickflow.android.feature.spotlist.components.SpotListSortBar
import com.pickflow.android.feature.spotlist.components.SpotListSortOption
import com.pickflow.android.feature.spotlist.components.SpotListUnauthorizedContent
import org.junit.Rule
import org.junit.Test

/**
 * iOS `SpotListSnapshotTests` 28케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * iOS는 SpotList를 정적 다크 토큰(gray95/gray90/...)으로 렌더하므로 light/dark가
 * 동일 — Android도 동일 컨텐츠를 양쪽 이름으로 record한다.
 * a11y(accessibilityExtraLarge)는 텍스트가 확대되므로 fontScale 2.0으로 렌더한다.
 */
class SpotListSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 852))

    // MARK: - Full screen — loaded

    @Test fun spot_list_loaded_mixed_light() = screen { SpotListLoadedGrid(mixedItems) }
    @Test fun spot_list_loaded_mixed_dark() = screen { SpotListLoadedGrid(mixedItems) }
    @Test fun spot_list_loaded_single_light() = screen { SpotListLoadedGrid(listOf(singleItem)) }
    @Test fun spot_list_loaded_single_dark() = screen { SpotListLoadedGrid(listOf(singleItem)) }

    // MARK: - Full screen — loading

    @Test fun spot_list_loading_light() = screen { SpotListLoadingContent() }
    @Test fun spot_list_loading_dark() = screen { SpotListLoadingContent() }

    // MARK: - Full screen — empty

    @Test fun spot_list_empty_light() = screen { SpotListEmptyContent() }
    @Test fun spot_list_empty_dark() = screen { SpotListEmptyContent() }
    @Test fun spot_list_empty_a11y_light() = screen(fontScale = 2.0f) { SpotListEmptyContent() }

    // MARK: - Full screen — failed

    @Test fun spot_list_failed_light() = screen { SpotListFailedContent("네트워크 오류") }
    @Test fun spot_list_failed_dark() = screen { SpotListFailedContent("네트워크 오류") }

    // MARK: - Full screen — unauthorized

    @Test fun spot_list_unauthorized_light() = screen { SpotListUnauthorizedContent() }
    @Test fun spot_list_unauthorized_dark() = screen { SpotListUnauthorizedContent() }
    @Test fun spot_list_unauthorized_a11y_light() = screen(fontScale = 2.0f) { SpotListUnauthorizedContent() }

    // MARK: - SortBar

    @Test fun spot_list_sortbar_nearest_collapsed_light() =
        sortBar(SpotListSortOption.Nearest, expanded = false)
    @Test fun spot_list_sortbar_nearest_collapsed_dark() =
        sortBar(SpotListSortOption.Nearest, expanded = false)
    @Test fun spot_list_sortbar_nearest_expanded_dark() =
        sortBar(SpotListSortOption.Nearest, expanded = true)
    @Test fun spot_list_sortbar_bookmark_collapsed_light() =
        sortBar(SpotListSortOption.Bookmark, expanded = false)
    @Test fun spot_list_sortbar_bookmark_expanded_dark() =
        sortBar(SpotListSortOption.Bookmark, expanded = true)

    // MARK: - Cell

    @Test fun spot_list_cell_sunset_bookmark_off_light() =
        cell(cellItem(SpotListMood.Sunset), isBookmarked = false, count = 12)
    @Test fun spot_list_cell_sunset_bookmark_off_dark() =
        cell(cellItem(SpotListMood.Sunset), isBookmarked = false, count = 12)
    @Test fun spot_list_cell_sunset_bookmark_on_light() =
        cell(cellItem(SpotListMood.Sunset), isBookmarked = true, count = 13)
    @Test fun spot_list_cell_reflection_bookmark_off_light() =
        cell(cellItem(SpotListMood.Reflection, name = "윤슬 스팟", distanceKm = 0.4), isBookmarked = false, count = 7)
    @Test fun spot_list_cell_reflection_bookmark_off_dark() =
        cell(cellItem(SpotListMood.Reflection, name = "윤슬 스팟", distanceKm = 0.4), isBookmarked = false, count = 7)
    @Test fun spot_list_cell_distance_nil_light() =
        cell(cellItem(SpotListMood.Sunset, distanceKm = null), isBookmarked = false, count = 12)
    @Test fun spot_list_cell_thumbnail_nil_light() =
        cell(cellItem(SpotListMood.Sunset, hasThumbnail = false), isBookmarked = false, count = 12)
    @Test fun spot_list_cell_long_name_truncate_light() =
        cell(cellItem(SpotListMood.Sunset, name = "아주아주 긴 한강 노을 스팟 이름 테스트 케이스"), isBookmarked = false, count = 12)
    @Test fun spot_list_cell_a11y_light() =
        cell(cellItem(SpotListMood.Sunset), isBookmarked = false, count = 12, heightDp = 420, fontScale = 2.0f)

    // MARK: - Fixtures

    private val mixedItems: List<SpotListGridItem>
        get() = listOf(
            SpotListGridItem(1, "한강 노을", SpotListMood.Sunset, distanceKm = 0.8),
            SpotListGridItem(2, "윤슬 한 바퀴", SpotListMood.Reflection, distanceKm = 1.3),
            SpotListGridItem(3, "응봉산 노을", SpotListMood.Sunset, distanceKm = 2.1),
            SpotListGridItem(4, "잠실 윤슬", SpotListMood.Reflection, distanceKm = 3.2),
            SpotListGridItem(5, "선유도 노을", SpotListMood.Sunset, distanceKm = 4.4),
            SpotListGridItem(6, "반포 윤슬", SpotListMood.Reflection, distanceKm = 5.0),
        )

    private val singleItem: SpotListGridItem
        get() = SpotListGridItem(1, "한강 노을", SpotListMood.Sunset, distanceKm = 0.8)

    private fun cellItem(
        mood: SpotListMood,
        name: String = "한강 노을 스팟",
        hasThumbnail: Boolean = true,
        distanceKm: Double? = 1.2,
    ) = SpotListGridItem(
        spotId = 1,
        name = name,
        mood = mood,
        hasThumbnail = hasThumbnail,
        distanceKm = distanceKm,
    )

    // MARK: - Renderers

    private fun screen(fontScale: Float = 1f, content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(device(393, 852, fontScale))
        paparazzi.snapshot { PickflowTheme { content() } }
    }

    private fun sortBar(sort: SpotListSortOption, expanded: Boolean) {
        paparazzi.unsafeUpdateConfig(device(393, if (expanded) 200 else 48))
        paparazzi.snapshot {
            PickflowTheme { SpotListSortBar(sort = sort, expanded = expanded) }
        }
    }

    private fun cell(
        item: SpotListGridItem,
        isBookmarked: Boolean,
        count: Int?,
        heightDp: Int = 280,
        fontScale: Float = 1f,
    ) {
        paparazzi.unsafeUpdateConfig(device(168, heightDp, fontScale))
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PickflowColors.gray95),
                    contentAlignment = Alignment.Center,
                ) {
                    SpotListCell(
                        item = item,
                        isBookmarked = isBookmarked,
                        bookmarkCount = count,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }

    private companion object {
        /** density 2.0(XHDPI)로 dp 좌표를 px로 환산한 DeviceConfig. */
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
