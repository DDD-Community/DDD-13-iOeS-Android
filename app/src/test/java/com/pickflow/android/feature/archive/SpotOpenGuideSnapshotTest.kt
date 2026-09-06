package com.pickflow.android.feature.archive

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.feature.archive.components.SpotOpenGuideSheetContent
import org.junit.Rule
import org.junit.Test

/** Figma `1201-9416` / `1201-9439` — 시트 본체. 삽화 이미지는 아직 자리만이다. */
class SpotOpenGuideSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun spot_open_guide_sheet() {
        paparazzi.snapshot {
            PickflowTheme {
                SpotOpenGuideSheetContent(onGoToOpen = {}, onConfirm = {})
            }
        }
    }
}
