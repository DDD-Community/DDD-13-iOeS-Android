package com.pickflow.android.feature.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
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

    @Test
    fun renders_mood_capsules() {
        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(
                    onOpenSpotDetail = {},
                    onOpenRegistration = {},
                    viewModel = viewModel(),
                )
            }
        }
        composeRule.onNodeWithText("노을").assertIsDisplayed()
        composeRule.onNodeWithText("윤슬").assertIsDisplayed()
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
