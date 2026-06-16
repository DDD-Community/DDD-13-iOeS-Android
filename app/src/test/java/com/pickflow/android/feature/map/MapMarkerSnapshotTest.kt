package com.pickflow.android.feature.map

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
import com.pickflow.android.feature.map.clustering.ClusterPinView
import com.pickflow.android.feature.map.clustering.MyClusterPinView
import com.pickflow.android.feature.map.clustering.SpotMarkerView
import org.junit.Rule
import org.junit.Test

/**
 * iOS `ClusterPinView` / `SpotMarkerView` / `MyClusterPinView` 와 동일한 시각 스펙을
 * Paparazzi 로 캡쳐. 마커가 sunsetOrange/검정 그라데이션/MY 라벨 등 핵심 디자인을
 * 회귀 없이 유지하는지 보장.
 */
class MapMarkerSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(120, 120))

    // MARK: - SpotMarkerView (큐레이션 단일)

    @Test fun marker_spot_unselected_dark() = snapshot { SpotMarkerView(isSelected = false) }
    @Test fun marker_spot_selected_dark() = snapshot { SpotMarkerView(isSelected = true) }

    // MARK: - ClusterPinView (큐레이션 클러스터) — sunsetOrange 단색.
    // 직경이 count 에 따라 4단계라 각 분기를 별도로 캡쳐.

    @Test fun marker_cluster_lt10_dark() = snapshot { ClusterPinView(count = 3) }
    @Test fun marker_cluster_lt50_dark() = snapshot { ClusterPinView(count = 27) }
    @Test fun marker_cluster_lt100_dark() = snapshot { ClusterPinView(count = 88) }
    @Test fun marker_cluster_ge100_dark() = snapshot { ClusterPinView(count = 150) }
    @Test fun marker_cluster_selected_dark() = snapshot { ClusterPinView(count = 5, isSelected = true) }

    // MARK: - MyClusterPinView (마이스팟 단일)

    @Test fun marker_myspot_unselected_dark() = snapshot { MyClusterPinView() }
    @Test fun marker_myspot_selected_dark() = snapshot { MyClusterPinView(isSelected = true) }

    // MARK: - Helpers

    private fun snapshot(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PickflowColors.gray95),
                    contentAlignment = Alignment.Center,
                ) { content() }
            }
        }
    }

    private companion object {
        fun device(wDp: Int, hDp: Int): DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = wDp * 2,
            screenHeight = hDp * 2,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = if (wDp > hDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
        )
    }
}
