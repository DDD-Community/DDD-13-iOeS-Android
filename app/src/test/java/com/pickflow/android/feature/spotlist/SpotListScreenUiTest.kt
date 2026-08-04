package com.pickflow.android.feature.spotlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.map.MoodFilter
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
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

    private fun loadedViewModel(): SpotListViewModel {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any()) } returns SpotPage(
            items = listOf(Spot("s1", "Spot One", SpotTheme.SUNSET, 0.0, 0.0)),
            page = 0,
            hasNext = false,
        )
        return SpotListViewModel(
            listService,
            mockk<BookmarkService>(relaxed = true),
            authService(),
            mockk(relaxed = true),
        )
    }

    /** PV59-LST1 — 무드 캡슐 4개가 햇살→윤슬→노을→야경 순으로 렌더된다. */
    @Test
    fun mood_filter_renders_four_moods_in_order() {
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = loadedViewModel())
            }
        }
        composeRule.onNodeWithTag("spotlist-mood").assertIsDisplayed()
        // 카드 배지에도 같은 라벨("노을" 등)이 있으므로 무드 행 안으로 한정해 조회한다.
        MoodFilter.entries.forEach { composeRule.moodCapsule(it.displayName).assertIsDisplayed() }
        assertEquals(listOf("햇살", "윤슬", "노을", "야경"), MoodFilter.entries.map { it.displayName })
    }

    /** PV59-LST2 — 초기 진입 시 아무 무드도 선택돼 있지 않다. */
    @Test
    fun mood_filter_starts_with_nothing_selected() {
        val vm = loadedViewModel()
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
        assertEquals(emptySet<SpotTheme>(), vm.themes.value)
    }

    /** PV59-LST3 — 두 무드를 동시에 선택하면 도메인 테마 2개가 담긴다. */
    @Test
    fun tapping_two_moods_selects_both_themes() {
        val vm = loadedViewModel()
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.moodCapsule("햇살").performClick()
        composeRule.moodCapsule("야경").performClick()
        composeRule.waitForIdle()
        assertEquals(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT), vm.themes.value)
    }

    /** PV59-LST4 — 전체 해제는 "필터 없음"이지 "빈 결과"가 아니다. */
    @Test
    fun clearing_all_moods_keeps_showing_results() {
        val vm = loadedViewModel()
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(onOpenSpotDetail = {}, onRequireLogin = {}, viewModel = vm)
            }
        }
        composeRule.moodCapsule("햇살").performClick()
        composeRule.moodCapsule("햇살").performClick()
        composeRule.waitForIdle()
        assertEquals(emptySet<SpotTheme>(), vm.themes.value)
        composeRule.onNodeWithTag("spotlist-grid").assertIsDisplayed()
    }

    /** 무드 행 안의 캡슐만 조회 — 카드 배지의 동일 라벨과 충돌하지 않게 한다. */
    private fun ComposeContentTestRule.moodCapsule(label: String): SemanticsNodeInteraction =
        onNode(hasText(label) and hasAnyAncestor(hasTestTag("spotlist-mood")))
}
