package com.pickflow.android.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.pickflow.android.common.designsystem.PickflowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotDetailBottomSheetUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun full_detail_removes_handle_area_and_swipe_down_still_collapses_sheet() {
        lateinit var sheetState: SheetState
        lateinit var scope: CoroutineScope
        val isFullDetail = mutableStateOf(false)

        composeRule.setContent {
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            scope = rememberCoroutineScope()
            PickflowTheme {
                SpotDetailBottomSheetSurface(
                    sheetState = sheetState,
                    isFullDetail = isFullDetail.value,
                    onDismissRequest = {},
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .testTag(FULL_DETAIL_CONTENT_TAG),
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            sheetState.currentValue == SheetValue.PartiallyExpanded
        }
        composeRule.onNodeWithTag(
            SPOT_DETAIL_SHEET_DRAG_HANDLE_TAG,
            useUnmergedTree = true,
        ).assertExists()

        composeRule.runOnIdle { scope.launch { sheetState.expand() } }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            sheetState.currentValue == SheetValue.Expanded
        }
        composeRule.runOnIdle { isFullDetail.value = true }
        composeRule.onNodeWithTag(
            SPOT_DETAIL_SHEET_DRAG_HANDLE_TAG,
            useUnmergedTree = true,
        ).assertDoesNotExist()

        composeRule.onNodeWithTag(FULL_DETAIL_CONTENT_TAG).performTouchInput {
            swipeDown(
                startY = height * 0.2f,
                endY = height * 0.5f,
                durationMillis = 1_500,
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            sheetState.currentValue != SheetValue.Expanded
        }
        assertEquals(SheetValue.PartiallyExpanded, sheetState.currentValue)
    }

    private companion object {
        const val FULL_DETAIL_CONTENT_TAG = "spotdetail-bottomsheet-full-test-content"
    }
}
