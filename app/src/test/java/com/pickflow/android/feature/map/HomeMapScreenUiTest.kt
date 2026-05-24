package com.pickflow.android.feature.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.Cluster
import com.pickflow.android.core.services.protocols.ClusteringService
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
class HomeMapScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel(
        spots: List<Spot> = emptyList(),
        clusters: List<Cluster> = emptyList(),
    ): HomeMapViewModel {
        val listService = mockk<SpotListService>()
        val clusteringService = mockk<ClusteringService>()
        coEvery { listService.fetch(any(), any(), any()) } returns SpotPage(spots, null)
        coEvery { clusteringService.cluster(any(), any()) } returns clusters
        return HomeMapViewModel(listService, clusteringService)
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
        val spot = Spot("s1", "한강 노을 스팟", SpotTheme.CAFE, 37.5, 127.0)
        val cluster = Cluster(37.5, 127.0, 1, listOf("s1"))
        val vm = viewModel(spots = listOf(spot), clusters = listOf(cluster))

        composeRule.setContent {
            PickflowTheme {
                HomeMapScreen(
                    onOpenSpotDetail = {},
                    onOpenRegistration = {},
                    viewModel = vm,
                )
            }
        }
        composeRule.waitForIdle() // load() 완료 — loadedSpots 채워짐
        composeRule.runOnIdle { vm.selectCluster(cluster) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotdetail-bottomsheet").assertIsDisplayed()
    }
}
