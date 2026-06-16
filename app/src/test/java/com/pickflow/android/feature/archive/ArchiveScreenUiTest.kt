package com.pickflow.android.feature.archive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
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

    private fun my(id: Long, status: MySpotStatus) = MySpot(
        id = id,
        name = "my$id",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = null,
        createdAt = "2026-01-01T00:00:00Z",
        status = status,
        bookmarkCount = 0,
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
    fun my_spots_tab_empty_shows_placeholder() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.MySpots,
                    archiveName = "나의 보관함",
                    mySpotState = LoadState.Empty,
                )
            }
        }
        composeRule.onNodeWithTag("archive-myspot-placeholder").assertIsDisplayed()
    }

    @Test
    fun my_spots_tab_loaded_shows_grid_cells_and_status_badges() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.MySpots,
                    archiveName = "나의 보관함",
                    mySpotState = LoadState.Loaded(
                        listOf(
                            my(1L, MySpotStatus.PENDING),
                            my(2L, MySpotStatus.REJECTED),
                            my(3L, MySpotStatus.PUBLISHED),
                        ),
                    ),
                )
            }
        }
        // LazyVerticalStaggeredGrid 의 일부 셀이 viewport 밖일 수 있어 assertExists 로 검증.
        // 배지는 merged semantics 트리에서 hidden 이라 unmerged tree 사용.
        composeRule.onNodeWithTag("archive-my-cell-1").assertExists()
        composeRule.onNodeWithTag("archive-my-cell-2").assertExists()
        composeRule.onNodeWithTag("archive-my-cell-3").assertExists()
        composeRule.onNodeWithTag("archive-my-badge-pending", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("archive-my-badge-rejected", useUnmergedTree = true).assertExists()
    }

    @Test
    fun my_spots_cell_click_invokes_onCellClick() {
        var lastId: Long? = null
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.MySpots,
                    archiveName = "나의 보관함",
                    mySpotState = LoadState.Loaded(listOf(my(7L, MySpotStatus.PUBLISHED))),
                    onCellClick = { lastId = it },
                )
            }
        }
        composeRule.onNodeWithTag("archive-my-cell-7").performClick()
        assert(lastId == 7L)
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
