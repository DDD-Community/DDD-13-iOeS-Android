package com.pickflow.android.feature.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.archive.components.ArchiveEmptyContent
import com.pickflow.android.feature.archive.components.ArchiveMySpotPlaceholderContent
import com.pickflow.android.feature.archive.components.ArchiveRenameDialogContent
import com.pickflow.android.feature.archive.components.ArchiveSignedOutContent
import com.pickflow.android.feature.archive.components.ArchiveTabBar
import org.junit.Rule
import org.junit.Test

/**
 * iOS `ArchiveSnapshotTests` 대응 Paparazzi 스냅샷. 정적 다크 토큰만 사용하므로
 * light/dark 동일 결과. 393x852 고정 디바이스.
 */
class ArchiveSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 852))

    private fun saved(id: Long, name: String = "한강 노을", distanceKm: Double? = 1.2) = SavedSpot(
        id = id,
        name = name,
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = distanceKm,
        savedAt = "2026-01-01T00:00:00Z",
        deleted = false,
    )

    // MARK: - Full screen states

    @Test fun archive_signedout_light() = snapshot { ArchiveSignedOutContent() }
    @Test fun archive_signedout_dark() = snapshot { ArchiveSignedOutContent() }

    @Test fun archive_empty_light() = snapshot { ArchiveEmptyContent() }
    @Test fun archive_empty_dark() = snapshot { ArchiveEmptyContent() }

    @Test fun archive_myspot_placeholder_light() = snapshot { ArchiveMySpotPlaceholderContent() }
    @Test fun archive_myspot_placeholder_dark() = snapshot { ArchiveMySpotPlaceholderContent() }

    @Test fun archive_loaded_savedspots_light() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Loaded(
                items = listOf(saved(1), saved(2, "응봉산 노을"), saved(3, "윤슬 한 바퀴", 0.6)),
                hasNext = false,
            ),
            selectedTab = ArchiveTab.SavedSpots,
            archiveName = "내 보관함",
        )
    }

    @Test fun archive_loaded_savedspots_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Loaded(
                items = listOf(saved(1), saved(2, "응봉산 노을"), saved(3, "윤슬 한 바퀴", 0.6)),
                hasNext = false,
            ),
            selectedTab = ArchiveTab.SavedSpots,
            archiveName = "내 보관함",
        )
    }

    @Test fun archive_loading_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Loading,
            selectedTab = ArchiveTab.SavedSpots,
            archiveName = "내 보관함",
        )
    }

    @Test fun archive_failed_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Failed("네트워크 오류"),
            selectedTab = ArchiveTab.SavedSpots,
            archiveName = "내 보관함",
        )
    }

    @Test fun archive_myspots_tab_empty_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Empty,
            selectedTab = ArchiveTab.MySpots,
            archiveName = "내 보관함",
            mySpotState = LoadState.Empty,
        )
    }

    @Test fun archive_myspots_loaded_mixed_status_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Empty,
            selectedTab = ArchiveTab.MySpots,
            archiveName = "내 보관함",
            mySpotState = LoadState.Loaded(
                listOf(
                    myStub(1L, "나만 보는 노을", MySpotStatus.DRAFT),
                    myStub(2L, "검수 대기중 스팟", MySpotStatus.PENDING),
                    myStub(3L, "재검수 대기중 스팟", MySpotStatus.RE_REVIEW_PENDING),
                    myStub(4L, "반려된 스팟", MySpotStatus.REJECTED),
                    myStub(5L, "공개된 스팟", MySpotStatus.PUBLISHED),
                ),
            ),
        )
    }

    @Test fun archive_private_saved_spot_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Loaded(
                items = listOf(
                    saved(41L, "비공개 노을 스팟").copy(
                        availability = SavedSpotAvailability.AUTHOR_PRIVATE,
                        isUserGenerated = true,
                    ),
                ),
                hasNext = false,
            ),
            selectedTab = ArchiveTab.SavedSpots,
            archiveName = "내 보관함",
        )
    }

    @Test fun archive_private_delete_dialog_dark() = snapshot {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PickflowColors.gray95),
            contentAlignment = Alignment.Center,
        ) {
            ArchivePrivateDeleteDialogContent()
        }
    }

    private fun myStub(id: Long, name: String, status: MySpotStatus) = MySpot(
        id = id,
        name = name,
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = 0.8,
        createdAt = "2026-01-01T00:00:00Z",
        status = status,
        bookmarkCount = 0,
    )

    @Test fun archive_toast_overlay_dark() = snapshot {
        ArchiveScreenContent(
            state = ArchiveLoadState.Empty,
            selectedTab = ArchiveTab.SavedSpots,
            archiveName = "내 보관함",
            toast = "북마크 해제에 실패했어요.",
        )
    }

    // MARK: - TabBar

    @Test fun archive_tabbar_savedspots_selected() = tabBar(ArchiveTab.SavedSpots)
    @Test fun archive_tabbar_myspots_selected() = tabBar(ArchiveTab.MySpots)

    // MARK: - Rename dialog

    @Test fun archive_rename_dialog_default_dark() = snapshot {
        ArchiveRenameDialogContent(initialName = "나의 보관함")
    }

    @Test fun archive_rename_dialog_empty_dark() = snapshot {
        ArchiveRenameDialogContent(initialName = "")
    }

    // MARK: - Helpers

    private fun tabBar(selected: ArchiveTab) {
        paparazzi.unsafeUpdateConfig(device(393, 60))
        paparazzi.snapshot {
            PickflowTheme { ArchiveTabBar(selectedTab = selected, onTabChange = {}) }
        }
    }

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot { PickflowTheme { content() } }
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
