package com.pickflow.android.feature.spotdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotDetailScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun actionsViewModel() =
        SpotDetailActionsViewModel(mockk<ExternalAppLauncher>(relaxed = true))

    @Test
    fun loaded_state_shows_spot_name_and_actions() {
        val spotService = mockk<SpotService>()
        val bookmarkService = mockk<BookmarkService>()
        val shareIntentService = mockk<ShareIntentService>(relaxed = true)
        coEvery { spotService.spot("s1") } returns
            Spot("s1", "상세 스팟", SpotTheme.SUNSET, 37.0, 127.0, address = "서울")
        coEvery { bookmarkService.isBookmarked("s1") } returns false
        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)

        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "s1",
                    onBack = {},
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                )
            }
        }
        composeRule.onNodeWithTag("spotdetail-screen").assertIsDisplayed()
        composeRule.onNodeWithText("상세 스팟").assertIsDisplayed()
        composeRule.onNodeWithTag("detail-bookmark").assertIsDisplayed()
    }

    @Test
    fun failed_state_shows_retry() {
        val spotService = mockk<SpotService>()
        val bookmarkService = mockk<BookmarkService>(relaxed = true)
        val shareIntentService = mockk<ShareIntentService>(relaxed = true)
        coEvery { spotService.spot(any()) } throws RuntimeException("not found")
        val vm = SpotDetailViewModel(spotService, bookmarkService, shareIntentService)

        composeRule.setContent {
            PickflowTheme {
                SpotDetailScreen(
                    spotId = "x",
                    onBack = {},
                    viewModel = vm,
                    actionsViewModel = actionsViewModel(),
                )
            }
        }
        composeRule.onNodeWithTag("state-failed").assertIsDisplayed()
    }
}
