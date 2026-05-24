package com.pickflow.android.feature.spotdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.spotdetail.components.SpotDetailData
import com.pickflow.android.feature.spotdetail.components.SpotDetailSheetContent
import com.pickflow.android.feature.spotdetail.components.SpotDetailTheme
import com.pickflow.android.feature.spotdetail.components.SpotSheetChrome
import org.junit.Rule
import org.junit.Test

/**
 * iOS `SpotDetailBottomSheetSnapshotTests` 7케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * iOS는 전부 다크 트레잇으로 렌더한다. 시트는 `.sizeThatFits`(폭 390 고정, 높이는
 * 컨텐츠) — Android는 iOS PNG 종횡비에서 산출한 디바이스 높이로 고정 렌더한다.
 */
class SpotDetailBottomSheetSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(390, 438))

    private val defaultSpot = SpotDetailData()
    private val longNameSpot = SpotDetailData(
        name = "잠원 한강공원 노을 명소 윤슬이 가장 아름다운 곳",
        theme = SpotDetailTheme.Reflection,
    )
    private val mineSpot = SpotDetailData(isMine = true)

    @Test fun sheetChrome_dark() {
        render(390, 230) {
            SpotSheetChrome {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(PickflowColors.gray95),
                )
            }
        }
    }

    @Test fun sheetContent_collapsedNotBookmarked_dark() =
        sheet(390, 438, defaultSpot, isBookmarked = false, expanded = false)

    @Test fun sheetContent_expandedNotBookmarked_dark() =
        sheet(390, 438, defaultSpot, isBookmarked = false, expanded = true)

    @Test fun sheetContent_collapsedBookmarked_dark() =
        sheet(390, 438, defaultSpot, isBookmarked = true, expanded = false)

    @Test fun sheetContent_longName_dark() =
        sheet(390, 468, longNameSpot, isBookmarked = false, expanded = false)

    @Test fun sheetContent_mySpot_dark() =
        sheet(390, 438, mineSpot, isBookmarked = false, expanded = false)

    @Test fun sheetContent_dynamicTypeAXL_dark() =
        sheet(390, 510, defaultSpot, isBookmarked = false, expanded = false, fontScale = 2.0f)

    // MARK: - Renderers

    private fun sheet(
        wDp: Int,
        hDp: Int,
        spot: SpotDetailData,
        isBookmarked: Boolean,
        expanded: Boolean,
        fontScale: Float = 1f,
    ) = render(wDp, hDp, fontScale) {
        SpotSheetChrome {
            SpotDetailSheetContent(
                spot = spot,
                isBookmarked = isBookmarked,
                addressExpanded = expanded,
            )
        }
    }

    private fun render(wDp: Int, hDp: Int, fontScale: Float = 1f, content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(device(wDp, hDp, fontScale))
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content()
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
