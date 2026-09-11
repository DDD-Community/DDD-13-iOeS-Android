package com.pickflow.android.feature.archive

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SpotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun saved(
        id: Long,
        likeCount: Long = 0L,
        availability: SavedSpotAvailability = SavedSpotAvailability.AVAILABLE,
    ) = SavedSpot(
        id = id,
        name = "spot$id",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        distanceKm = null,
        likeCount = likeCount,
        savedAt = "2026-01-01T00:00:00Z",
        deleted = false,
        availability = availability,
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
        // 공통 SignedOutLoginContent 로 대체됨 — 카카오 버튼은 텍스트로 검증.
        // (Apple 로그인은 임시 비활성화 상태)
        composeRule.onNodeWithText("카카오로 로그인").assertIsDisplayed()
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

    // MARK: - PV-144

    /**
     * 북마크 아이콘을 눌러도 셀 클릭(상세 이동)만 되던 버그.
     *
     * 히트영역이 썸네일 우상단의 보이지 않는 32dp Box 였는데 실제 아이콘은 메타 행에
     * 있어 위치가 어긋나 있었다. 이제 아이콘 자체가 IconButton 이다.
     */
    @Test
    fun tapping_the_bookmark_icon_toggles_instead_of_navigating() {
        var bookmarked: Long? = null
        var navigated: Long? = null
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(items = listOf(saved(1)), hasNext = false),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                    onCellClick = { navigated = it },
                    onBookmarkTap = { bookmarked = it },
                )
            }
        }

        composeRule.onNodeWithTag("spotcell-bookmark-1").performClick()

        assertEquals(1L, bookmarked)
        assertNull(navigated)
    }

    /** 셀의 다른 곳을 누르면 그대로 상세로 이동해야 한다. */
    @Test
    fun tapping_the_cell_body_still_navigates() {
        var bookmarked: Long? = null
        var navigated: Long? = null
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(items = listOf(saved(1)), hasNext = false),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                    onCellClick = { navigated = it },
                    onBookmarkTap = { bookmarked = it },
                )
            }
        }

        composeRule.onNodeWithTag("archive-cell-1").performClick()

        assertEquals(1L, navigated)
        assertNull(bookmarked)
    }

    /** 저장 리스트도 탐색 리스트처럼 "무드 · 추천 N" 을 보여준다. */
    @Test
    fun saved_cell_shows_like_count() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(items = listOf(saved(1, likeCount = 12)), hasNext = false),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithText("추천 12").assertIsDisplayed()
    }

    /** 비공개 스팟 셀은 안내 문구로 덮이므로 북마크 토글 자리를 두지 않는다. */
    @Test
    fun private_saved_cell_has_no_bookmark_button() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(
                        items = listOf(saved(1, availability = SavedSpotAvailability.AUTHOR_PRIVATE)),
                        hasNext = false,
                    ),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        composeRule.onNodeWithTag("spotcell-bookmark-1").assertDoesNotExist()
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
        composeRule.onNodeWithTag("archive-my-badge-in-review", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("archive-my-badge-rejected", useUnmergedTree = true).assertExists()
    }

    @Test
    fun my_spots_cell_click_invokes_onMyCellClick() {
        // "나만의 스팟" 탭은 저장된 스팟과 다른 화면(오픈 관리)으로 간다 — 콜백이 분리돼 있다.
        var lastId: Long? = null
        var savedTabId: Long? = null
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Empty,
                    selectedTab = ArchiveTab.MySpots,
                    archiveName = "나의 보관함",
                    mySpotState = LoadState.Loaded(listOf(my(7L, MySpotStatus.PUBLISHED))),
                    onCellClick = { savedTabId = it },
                    onMyCellClick = { lastId = it },
                )
            }
        }
        composeRule.onNodeWithTag("archive-my-cell-7").performClick()
        assert(lastId == 7L)
        assert(savedTabId == null)
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

    /** PV59-ARC1 — 저장 카드 셀의 무드 배지가 4종 각각으로 렌더된다. */
    @Test
    fun saved_cards_render_all_four_mood_badges() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(
                        items = SpotTheme.entries.mapIndexed { index, theme ->
                            saved(index.toLong()).copy(theme = theme)
                        },
                        hasNext = false,
                    ),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }
        listOf("햇살", "윤슬", "노을", "야경").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    /**
     * 스티키 탭바가 고정되는 경계(커버가 막 스크롤아웃되는 지점)에서 그리드 안의 탭바와
     * 상단 고정 탭바가 겹쳐 "저장된 스팟"이 두 번 보이던 버그.
     */
    @Test
    fun tabbar_is_not_duplicated_at_sticky_threshold() {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = ArchiveLoadState.Loaded(
                        items = (1L..20L).map { saved(it) },
                        hasNext = false,
                    ),
                    selectedTab = ArchiveTab.SavedSpots,
                    archiveName = "나의 보관함",
                )
            }
        }

        // index 1 = 탭바 아이템. 이게 그리드 최상단에 오는 순간이 고정 경계다.
        composeRule.onNodeWithTag("archive-scroll").performScrollToIndex(1)

        composeRule.onAllNodesWithTag("archive-tabbar").assertCountEquals(1)
        composeRule.onNodeWithTag("archive-tabbar").assertIsDisplayed()
    }
}
