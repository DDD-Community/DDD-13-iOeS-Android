package com.pickflow.android.snapshot

import androidx.compose.material3.Text
import app.cash.paparazzi.Paparazzi
import com.pickflow.android.common.designsystem.PickflowTheme
import org.junit.Rule
import org.junit.Test

/** Paparazzi 툴체인 동작 검증용 스모크 테스트. */
class PaparazziSetupTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun smoke() {
        paparazzi.snapshot {
            PickflowTheme {
                Text("Pickflow Paparazzi OK")
            }
        }
    }
}
