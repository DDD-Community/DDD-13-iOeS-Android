package com.pickflow.android.feature.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.impl.InMemoryMoodFilterStore
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
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
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeMapScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(spots: List<Spot> = emptyList()): HomeMapViewModel {
        val listService = mockk<SpotListService>()
        val mapService = mockk<SpotMapService>(relaxed = true)
        val locationService = mockk<LocationService>(relaxed = true)
        coEvery { listService.fetch(any(), any()) } returns SpotPage(items = spots, page = 0, hasNext = false)
        return HomeMapViewModel(
            listService,
            mapService,
            locationService,
            mockk<SpotService>(relaxed = true),
            mockk<AuthService>(relaxed = true),
            mockk<BookmarkService>(relaxed = true),
            mockk<ExternalAppLauncher>(relaxed = true),
        InMemoryMoodFilterStore(),
        )
    }

    @Test
    fun renders_map_with_action_buttons() {
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(
                    onOpenSpotDetail = {},
                    onOpenRegistration = {},
                    viewModel = viewModel(),
                )
            }
        }
        composeRule.onNodeWithTag("homemap-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("homemap-toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("homemap-register").assertIsDisplayed()
    }

    /** PV59-MAP1/MAP2 — 무드 캡슐 4개가 햇살→윤슬→노을→야경 순으로 렌더된다. */
    @Test
    fun mood_filter_renders_four_moods_in_order() {
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(
                    onOpenSpotDetail = {},
                    onOpenRegistration = {},
                    viewModel = viewModel(),
                )
            }
        }
        MoodFilter.entries.forEach { composeRule.onNodeWithText(it.displayName).assertIsDisplayed() }
        assertEquals(listOf("햇살", "윤슬", "노을", "야경"), MoodFilter.entries.map { it.displayName })
    }

    /** PV59-MAP3 — 초기 진입 시 아무 무드도 선택돼 있지 않다. */
    @Test
    fun mood_filter_starts_with_nothing_selected() {
        val vm = viewModel()
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(onOpenSpotDetail = {}, onOpenRegistration = {}, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
        assertEquals(emptySet<MoodFilter>(), vm.selectedMoods.value)
    }

    /** PV59-MAP4 — 두 무드를 동시에 선택할 수 있다(다중선택). */
    @Test
    fun tapping_two_moods_selects_both() {
        val vm = viewModel()
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(onOpenSpotDetail = {}, onOpenRegistration = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithText("햇살").performClick()
        composeRule.onNodeWithText("야경").performClick()
        composeRule.waitForIdle()
        assertEquals(setOf(MoodFilter.Sunlight, MoodFilter.Night), vm.selectedMoods.value)
    }

    /** PV59-MAP5 — 선택된 캡슐 재탭 시 그 하나만 해제된다. */
    @Test
    fun retapping_a_selected_mood_clears_only_that_one() {
        val vm = viewModel()
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(onOpenSpotDetail = {}, onOpenRegistration = {}, viewModel = vm)
            }
        }
        composeRule.onNodeWithText("햇살").performClick()
        composeRule.onNodeWithText("야경").performClick()
        composeRule.onNodeWithText("햇살").performClick()
        composeRule.waitForIdle()
        assertEquals(setOf(MoodFilter.Night), vm.selectedMoods.value)
    }

    // --- 지역 필터 ---

    /** 로고 우측에 현재 적용 중인 지역명이 보인다. */
    @Test
    fun header_shows_applied_region_name() {
        setScreen(viewModel())
        composeRule.onNodeWithTag("homemap-region").assertIsDisplayed()
        composeRule.onNodeWithText("서울").assertIsDisplayed()
    }

    /** 지역명 탭 → 지역 선택 바텀시트 노출. */
    @Test
    fun tapping_region_opens_picker_sheet() {
        setScreen(viewModel())
        composeRule.onNodeWithTag("homemap-region").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("어느 지역을 둘러볼까요?").assertIsDisplayed()
    }

    /** 다른 지역 선택 후 [적용하기] → 지역이 갱신되고 지도 이동 좌표가 나간다. */
    @Test
    fun selecting_another_region_and_applying_updates_region() {
        val vm = viewModel()
        setScreen(vm)
        composeRule.onNodeWithTag("homemap-region").performClick()
        composeRule.onNodeWithText("대전").performClick()
        composeRule.onNodeWithTag("region-picker-apply").performClick()
        composeRule.waitForIdle()

        assertEquals(Region.Daejeon, vm.region.value)
        assertEquals(Region.Daejeon.center, vm.regionTarget.value)
    }

    /** 다른 지역을 골라도 [취소] 면 기존 지역이 유지된다. */
    @Test
    fun cancelling_keeps_the_previously_applied_region() {
        val vm = viewModel()
        setScreen(vm)
        composeRule.onNodeWithTag("homemap-region").performClick()
        composeRule.onNodeWithText("대전").performClick()
        composeRule.onNodeWithTag("region-picker-cancel").performClick()
        composeRule.waitForIdle()

        assertEquals(Region.Seoul, vm.region.value)
        assertEquals(null, vm.regionTarget.value)
    }

    /** 재호출 시 현재 적용 중인 지역이 선택된 상태로 뜬다(취소로 버린 선택이 남지 않는다). */
    @Test
    fun reopening_the_sheet_resets_pending_selection_to_applied_region() {
        val vm = viewModel()
        setScreen(vm)
        composeRule.onNodeWithTag("homemap-region").performClick()
        composeRule.onNodeWithText("대전").performClick()
        composeRule.onNodeWithTag("region-picker-cancel").performClick()
        composeRule.waitForIdle()

        // 다시 열어 그대로 [적용하기] → 버려진 대전이 아니라 서울이 유지돼야 한다.
        composeRule.onNodeWithTag("homemap-region").performClick()
        composeRule.onNodeWithTag("region-picker-apply").performClick()
        composeRule.waitForIdle()

        assertEquals(Region.Seoul, vm.region.value)
    }

    private fun setScreen(vm: HomeMapViewModel) {
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(onOpenSpotDetail = {}, onOpenRegistration = {}, viewModel = vm)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun cluster_tap_shows_bottom_sheet() {
        val spot = Spot("1", "한강 노을 스팟", SpotTheme.SUNSET, 37.5, 127.0)
        val vm = viewModel(spots = listOf(spot))
        val cluster = Cluster(37.5, 127.0, 1, listOf("1"))

        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(
                    onOpenSpotDetail = {},
                    onOpenRegistration = {},
                    viewModel = vm,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { vm.selectCluster(cluster) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotdetail-bottomsheet").assertIsDisplayed()
    }
}
