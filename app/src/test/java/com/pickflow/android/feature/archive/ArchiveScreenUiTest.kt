package com.pickflow.android.feature.archive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArchiveScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun saved(id: Long) = SavedSpot(
        id = id,
        name = "spot$id",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = null,
        savedAt = "2026-01-01T00:00:00Z",
        deleted = false,
    )

    @Test
    fun signed_out_state_shows_login_buttons() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.SignedOut,
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithTag("archive-signedout").assertIsDisplayed()
        composeRule.onNodeWithTag("archive-kakao").assertIsDisplayed()
        composeRule.onNodeWithTag("archive-apple").assertIsDisplayed()
    }

    @Test
    fun empty_state_shows_explore_cta() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithTag("archive-empty").assertIsDisplayed()
        composeRule.onNodeWithTag("archive-empty-explore").assertIsDisplayed()
    }

    @Test
    fun loaded_state_shows_grid_cells() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(items = listOf(saved(1), saved(2)), hasNext = false),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithTag("archive-scroll").assertIsDisplayed()
        composeRule.onNodeWithTag("archive-cell-1").assertIsDisplayed()
    }

    @Test
    fun my_spots_tab_shows_placeholder() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.MySpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithTag("archive-myspot-placeholder").assertIsDisplayed()
    }

    @Test
    fun tab_change_invokes_callback() {
        var lastTab: ArchiveTab? = null
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                    onTabChange = { lastTab = it },
                )
            }
        }
        composeRule.onNodeWithTag("archive-tab-myspots").performClick()
        assert(lastTab == ArchiveTab.MySpots)
    }

    @Test
    fun failed_state_shows_failed_message() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Failed("네트워크 오류"),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithTag("archive-failed").assertIsDisplayed()
    }

    @Test
    fun toast_overlay_shows_when_toast_present() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                    toast = "북마크 해제에 실패했어요.",
                )
            }
        }
        composeRule.onNodeWithTag("archive-toast").assertIsDisplayed()
    }
}
