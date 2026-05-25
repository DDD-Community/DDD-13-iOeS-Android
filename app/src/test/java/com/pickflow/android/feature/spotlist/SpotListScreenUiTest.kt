package com.pickflow.android.feature.spotlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
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
class SpotListScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun authService() = mockk<AuthService>(relaxed = true)

    @Test
    fun loaded_state_shows_grid() {
        val listService = mockk<SpotListService>()
        val bookmarkService = mockk<BookmarkService>(relaxed = true)
        coEvery { listService.fetch(any(), any(), any()) } returns SpotPage(
            items = listOf(Spot("s1", "Spot One", SpotTheme.SUNSET, 0.0, 0.0)),
            nextCursor = null,
        )
        val vm = SpotListViewModel(listService, bookmarkService, authService())

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithTag("spotlist-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlist-grid").assertIsDisplayed()
    }

    @Test
    fun empty_state_shows_empty_message() {
        val listService = mockk<SpotListService>()
        val bookmarkService = mockk<BookmarkService>(relaxed = true)
        coEvery { listService.fetch(any(), any(), any()) } returns SpotPage(emptyList(), null)
        val vm = SpotListViewModel(listService, bookmarkService, authService())

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithTag("state-empty").assertIsDisplayed()
    }
}
