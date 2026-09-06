package com.pickflow.android.feature.home

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
import com.pickflow.android.app.navigation.HomeTab
import com.pickflow.android.common.designsystem.PickflowColors
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.ReviewResult
import org.junit.Rule
import org.junit.Test

class HomeReviewResultSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = device(393, 96))

    @Test
    fun home_bottom_navigation_saved_indicator() = snapshot(393, 96, Alignment.BottomCenter) {
        HomeBottomNavigation(
            selectedTab = HomeTab.EXPLORE,
            hasSavedIndicator = true,
            onTabSelected = {},
        )
    }

    @Test
    fun review_result_snackbar_approved() = snapshot(393, 112) {
        ReviewResultSnackbar(
            result = result(ReviewDecision.APPROVED),
            onOpenResult = {},
            onClose = {},
        )
    }

    @Test
    fun review_result_snackbar_rejected() = snapshot(393, 112) {
        ReviewResultSnackbar(
            result = result(ReviewDecision.REJECTED),
            onOpenResult = {},
            onClose = {},
        )
    }

    private fun snapshot(
        widthDp: Int,
        heightDp: Int,
        alignment: Alignment = Alignment.Center,
        content: @Composable () -> Unit,
    ) {
        paparazzi.unsafeUpdateConfig(device(widthDp, heightDp))
        paparazzi.snapshot {
            PickflowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PickflowColors.gray95),
                    contentAlignment = alignment,
                ) {
                    content()
                }
            }
        }
    }

    private fun result(decision: ReviewDecision) = ReviewResult(
        resultId = 1L,
        spotId = 41L,
        decision = decision,
        occurredAt = "2026-08-06T12:00:00Z",
    )

    private companion object {
        fun device(widthDp: Int, heightDp: Int): DeviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = widthDp * 2,
            screenHeight = heightDp * 2,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = if (widthDp > heightDp) {
                ScreenOrientation.LANDSCAPE
            } else {
                ScreenOrientation.PORTRAIT
            },
        )
    }
}
