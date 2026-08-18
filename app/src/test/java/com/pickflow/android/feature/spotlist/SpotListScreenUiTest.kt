package com.pickflow.android.feature.spotlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LocationService
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
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpotListScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun authService() = mockk<AuthService>(relaxed = true)

    @Test
    fun loaded_state_shows_grid() {
        val listService = mockk<SpotListService>()
        val bookmarkService = mockk<BookmarkService>(relaxed = true)
        coEvery { listService.fetch(any(), any(), any(), any()) } returns SpotPage(
            items = listOf(Spot("s1", "Spot One", SpotTheme.SUNSET, 0.0, 0.0)),
            page = 0,
            hasNext = false,
        )
        val vm = SpotListViewModel(listService, bookmarkService, authService(), mockk(relaxed = true))

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
        coEvery { listService.fetch(any(), any(), any(), any()) } returns SpotPage(items = emptyList(), page = 0, hasNext = false)
        val vm = SpotListViewModel(listService, bookmarkService, authService(), mockk(relaxed = true))

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithTag("state-empty").assertIsDisplayed()
    }

    @Test
    fun cell_meta_shows_mood_and_like_count() {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any()) } returns SpotPage(
            items = listOf(
                Spot("s1", "윤슬 스팟", SpotTheme.YUNSEUL, 0.0, 0.0, likeCount = 34),
            ),
            page = 0,
            hasNext = false,
        )
        val vm = SpotListViewModel(
            listService, mockk<BookmarkService>(relaxed = true), authService(), mockk(relaxed = true),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithText("추천 34").assertIsDisplayed()
        composeRule.onNodeWithText("북마크 34").assertDoesNotExist()
    }

    @Test
    fun bookmarked_spot_from_response_renders_as_bookmarked() {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any()) } returns SpotPage(
            items = listOf(Spot("s1", "Spot One", SpotTheme.SUNSET, 0.0, 0.0, isBookmarked = true)),
            page = 0,
            hasNext = false,
        )
        val vm = SpotListViewModel(
            listService, mockk<BookmarkService>(relaxed = true), authService(), mockk(relaxed = true),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithContentDescription("북마크 해제").assertIsDisplayed()
    }

    @Test
    fun sort_header_shows_recommended_label() {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any()) } returns SpotPage(emptyList(), 0, false)
        val vm = SpotListViewModel(
            listService, mockk<BookmarkService>(relaxed = true), authService(), mockk(relaxed = true),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithText("추천 순").assertIsDisplayed()
    }
}
