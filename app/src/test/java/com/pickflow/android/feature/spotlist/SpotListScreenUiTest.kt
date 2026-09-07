package com.pickflow.android.feature.spotlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.impl.FakeNewFeatureGuide
import com.pickflow.android.core.services.impl.InMemoryMoodFilterStore
import com.pickflow.android.core.services.impl.DefaultRegionStore
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import com.pickflow.android.feature.map.MoodFilter
import com.pickflow.android.feature.map.NewFeatureBadgeViewModel
import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } returns SpotPage(
            items = listOf(Spot("s1", "Spot One", SpotTheme.SUNSET, 0.0, 0.0)),
            page = 0,
            hasNext = false,
        )
        val vm = SpotListViewModel(listService, bookmarkService, authService(), mockk(relaxed = true), InMemoryMoodFilterStore(), DefaultRegionStore(mockk(relaxed = true)))

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.onNodeWithTag("spotlist-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlist-grid").assertIsDisplayed()
    }

    @Test
    fun empty_state_shows_empty_message() {
        val listService = mockk<SpotListService>()
        val bookmarkService = mockk<BookmarkService>(relaxed = true)
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } returns SpotPage(items = emptyList(), page = 0, hasNext = false)
        val vm = SpotListViewModel(listService, bookmarkService, authService(), mockk(relaxed = true), InMemoryMoodFilterStore(), DefaultRegionStore(mockk(relaxed = true)))

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.onNodeWithTag("state-empty").assertIsDisplayed()
    }

    @Test
    fun cell_meta_shows_mood_and_like_count() {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } returns SpotPage(
            items = listOf(
                Spot("s1", "윤슬 스팟", SpotTheme.YUNSEUL, 0.0, 0.0, likeCount = 34),
            ),
            page = 0,
            hasNext = false,
        )
        val vm = SpotListViewModel(
            listService,
            mockk<BookmarkService>(relaxed = true),
            authService(),
            mockk(relaxed = true),
            InMemoryMoodFilterStore(),
            DefaultRegionStore(mockk(relaxed = true)),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.onNodeWithText("추천 34").assertIsDisplayed()
        composeRule.onNodeWithText("북마크 34").assertDoesNotExist()
    }

    @Test
    fun bookmarked_spot_from_response_renders_as_bookmarked() {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } returns SpotPage(
            items = listOf(Spot("s1", "Spot One", SpotTheme.SUNSET, 0.0, 0.0, isBookmarked = true)),
            page = 0,
            hasNext = false,
        )
        val vm = SpotListViewModel(
            listService,
            mockk<BookmarkService>(relaxed = true),
            authService(),
            mockk(relaxed = true),
            InMemoryMoodFilterStore(),
            DefaultRegionStore(mockk(relaxed = true)),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.onNodeWithContentDescription("북마크 해제").assertIsDisplayed()
    }

    @Test
    fun sort_header_shows_recommended_label() {
        val listService = mockk<SpotListService>()
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } returns SpotPage(emptyList(), 0, false)
        val vm = SpotListViewModel(
            listService,
            mockk<BookmarkService>(relaxed = true),
            authService(),
            mockk(relaxed = true),
            InMemoryMoodFilterStore(),
            DefaultRegionStore(mockk(relaxed = true)),
        )

        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.onNodeWithText("추천 순").assertIsDisplayed()
    }

    private fun loadedViewModel(
        count: Int = 1,
        /** 넘기면 재조회가 여기서 멈춘다 — complete 하기 전까지 화면은 LoadState.Loading. */
        refetchGate: CompletableDeferred<Unit>? = null,
        /** 필터를 걸면 결과 수가 달라진다 — 재조회 응답을 따로 준다. */
        refetchCount: Int = count,
    ): SpotListViewModel {
        val listService = mockk<SpotListService>()
        fun pageOf(n: Int, prefix: String) = SpotPage(
            items = (1..n).map { Spot("$prefix$it", "Spot $it", SpotTheme.SUNSET, 0.0, 0.0) },
            page = 0,
            hasNext = false,
        )
        val page = pageOf(count, "s")
        val refetched = pageOf(refetchCount, "f")
        // 첫 로드는 즉시, 재조회(필터 변경)만 게이트로 붙잡아 LoadState.Loading 프레임을 만든다.
        var calls = 0
        coEvery { listService.fetch(any(), any(), any(), any(), any()) } coAnswers {
            calls++
            if (calls == 1) return@coAnswers page
            refetchGate?.await()
            refetched
        }
        return SpotListViewModel(
            listService,
            mockk<BookmarkService>(relaxed = true),
            authService(),
            mockk(relaxed = true),
            InMemoryMoodFilterStore(),
            DefaultRegionStore(mockk(relaxed = true)),
        )
    }

    /** PV59-LST1 — 무드 캡슐 4개가 햇살→윤슬→노을→야경 순으로 렌더된다. */
    @Test
    fun mood_filter_renders_four_moods_in_order() {
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = loadedViewModel(),
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
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
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
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
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.moodCapsule("햇살").performClick()
        composeRule.moodCapsule("야경").performClick()
        composeRule.waitForIdle()
        assertEquals(setOf(SpotTheme.SUNLIGHT, SpotTheme.NIGHT_VIEW), vm.themes.value)
    }

    /** PV59-LST4 — 전체 해제는 "필터 없음"이지 "빈 결과"가 아니다. */
    @Test
    fun clearing_all_moods_keeps_showing_results() {
        val vm = loadedViewModel()
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    // 화면 기본값이 hiltViewModel() 이라 Hilt 없는 Robolectric 에서는 터진다.
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.moodCapsule("햇살").performClick()
        composeRule.moodCapsule("햇살").performClick()
        composeRule.waitForIdle()
        assertEquals(emptySet<SpotTheme>(), vm.themes.value)
        composeRule.onNodeWithTag("spotlist-grid").assertIsDisplayed()
    }

    /** PV86-1 — 정렬 드롭다운은 로고 행이 아니라 무드 필터 행 **아래**에 놓인다. */
    @Test
    fun sort_selector_sits_below_mood_filter_row() {
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = loadedViewModel(),
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        val regionTop = composeRule.onNodeWithTag("spotlist-region").getUnclippedBoundsInRoot().top
        val moodBounds = composeRule.onNodeWithTag("spotlist-mood").getUnclippedBoundsInRoot()
        val sortTop = composeRule.onNodeWithTag("spotlist-sort-toggle").getUnclippedBoundsInRoot().top

        assertTrue("로고 행이 무드 행보다 위", regionTop < moodBounds.top)
        assertTrue("정렬은 무드 행 아래", sortTop >= moodBounds.bottom)
    }

    /** PV86-2 — 스크롤하면 로고 행과 정렬 바는 밀려 올라가고 무드 행만 상단에 고정된다. */
    @Test
    fun scrolling_keeps_only_mood_filter_row_pinned() {
        val vm = loadedViewModel(count = 24)
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.waitForIdle()
        val moodTopBefore = composeRule.onNodeWithTag("spotlist-mood").getUnclippedBoundsInRoot().top

        composeRule.onNodeWithTag("spotlist-grid").performScrollToIndex(20)
        composeRule.waitForIdle()

        val moodTopAfter = composeRule.onNodeWithTag("spotlist-mood").getUnclippedBoundsInRoot().top
        val regionTopAfter = composeRule.onNodeWithTag("spotlist-region").getUnclippedBoundsInRoot().top

        assertTrue("무드 행이 위로 올라붙는다", moodTopAfter < moodTopBefore)
        assertTrue("무드 행은 화면 밖으로 나가지 않는다", moodTopAfter >= 0.dp)
        assertTrue("로고 행은 무드 행 뒤로 밀려 올라간다", regionTopAfter < moodTopAfter)
    }

    /** PV86-3 — 스크롤해 접힌 상태에서 무드를 토글해도 무드 행은 상단에 붙어 있다. */
    @Test
    fun toggling_mood_keeps_the_header_collapsed() {
        // 실기기처럼 재조회가 LoadState.Loading 을 거쳐야(= 그리드가 컴포지션에서 빠져야)
        // 회귀가 재현된다. 게이트로 그 순간을 붙잡는다.
        val gate = CompletableDeferred<Unit>()
        val vm = loadedViewModel(count = 24, refetchGate = gate, refetchCount = 12)
        composeRule.setContent {
            PickflowTheme {
                SpotListScreen(
                    onOpenSpotDetail = {},
                    onRequireLogin = {},
                    viewModel = vm,
                    newFeatureBadgeViewModel = NewFeatureBadgeViewModel(FakeNewFeatureGuide()),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlist-grid").performScrollToIndex(20)
        composeRule.waitForIdle()
        val moodTopCollapsed = composeRule.onNodeWithTag("spotlist-mood").getUnclippedBoundsInRoot().top

        composeRule.moodCapsule("햇살").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlist-grid").assertDoesNotExist()
        gate.complete(Unit)
        composeRule.waitForIdle()

        val moodTopAfterToggle = composeRule.onNodeWithTag("spotlist-mood").getUnclippedBoundsInRoot().top
        assertEquals(moodTopCollapsed, moodTopAfterToggle)
    }

    /** 무드 행 안의 캡슐만 조회 — 카드 배지의 동일 라벨과 충돌하지 않게 한다. */
    private fun ComposeContentTestRule.moodCapsule(label: String): SemanticsNodeInteraction =
        onNode(hasText(label) and hasAnyAncestor(hasTestTag("spotlist-mood")))
}
