package com.pickflow.android.feature.map.clustering

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.pickflow.android.common.designsystem.PickflowTheme
import org.junit.Rule
import org.junit.Test

/**
 * iOS `MapClusteringSnapshotTests` 20케이스 1:1 대응 Paparazzi 스냅샷.
 *
 * 핀 컴포넌트는 작은 원형 뷰 — iOS `.sizeThatFits`처럼 디바이스를 핀 지름에 맞춘다.
 * 핀 내용물은 정적 색이라 light/dark 렌더가 동일 — 양쪽 이름으로 record한다.
 * a11y(accessibility3)는 fontScale 2.0으로 렌더한다(텍스트 확대).
 */
class MapClusteringSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(56, 56))

    // MARK: - ClusterPinView

    @Test fun clusterPin_count2_light() = clusterPin(2)
    @Test fun clusterPin_count2_dark() = clusterPin(2)
    @Test fun clusterPin_count12_light() = clusterPin(12)
    @Test fun clusterPin_count12_dark() = clusterPin(12)
    @Test fun clusterPin_count75_light() = clusterPin(75)
    @Test fun clusterPin_count75_dark() = clusterPin(75)
    @Test fun clusterPin_count150_light() = clusterPin(150)
    @Test fun clusterPin_count150_dark() = clusterPin(150)
    @Test fun clusterPin_count12_a11y_light() = clusterPin(12, fontScale = 2.0f)
    @Test fun clusterPin_count12_selected_light() = clusterPin(12, isSelected = true)
    @Test fun clusterPin_count12_selected_dark() = clusterPin(12, isSelected = true)

    // MARK: - MyClusterPinView

    @Test fun myClusterPin_light() = myClusterPin()
    @Test fun myClusterPin_dark() = myClusterPin()
    @Test fun myClusterPin_a11y_dark() = myClusterPin(fontScale = 2.0f)
    @Test fun myClusterPin_selected_light() = myClusterPin(isSelected = true)
    @Test fun myClusterPin_selected_dark() = myClusterPin(isSelected = true)

    // MARK: - SpotMarkerView

    @Test fun spotMarker_default_light() = spotMarker(isSelected = false)
    @Test fun spotMarker_default_dark() = spotMarker(isSelected = false)
    @Test fun spotMarker_selected_light() = spotMarker(isSelected = true)
    @Test fun spotMarker_selected_dark() = spotMarker(isSelected = true)

    // MARK: - Renderers

    private fun clusterPin(count: Int, isSelected: Boolean = false, fontScale: Float = 1f) {
        val d = clusterPinDiameter(count)
        render(d, fontScale) { ClusterPinView(count = count, isSelected = isSelected) }
    }

    private fun myClusterPin(isSelected: Boolean = false, fontScale: Float = 1f) {
        render(56, fontScale) { MyClusterPinView(isSelected = isSelected) }
    }

    private fun spotMarker(isSelected: Boolean) {
        render(44, 1f) { SpotMarkerView(isSelected = isSelected) }
    }

    private fun render(diameter: Int, fontScale: Float, content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(device(diameter, diameter, fontScale))
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
