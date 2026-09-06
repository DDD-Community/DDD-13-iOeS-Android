package com.pickflow.android.feature.archive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Archive
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SavedSpotPage
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
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
class ArchiveSpotOpenScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun mySpot(
        id: Long,
        status: MySpotStatus,
        wasPublished: Boolean = false,
        likeCount: Long? = 34L,
    ) = MySpot(
        id = id,
        name = "MY 스팟 $id",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 37.5,
        longitude = 127.0,
        distanceKm = null,
        createdAt = "2026-08-06T00:00:00Z",
        status = status,
        bookmarkCount = 0L,
        likeCount = likeCount,
        wasPublished = wasPublished,
    )

    private fun privateSavedSpot(id: Long = PRIVATE_SPOT_ID) = SavedSpot(
        id = id,
        name = "비공개 노을 스팟",
        theme = SpotTheme.SUNSET,
        imageUrl = "https://cdn.example.com/$id.jpg",
        latitude = 37.5,
        longitude = 127.0,
        distanceKm = 1.2,
        savedAt = "2026-08-06T00:00:00Z",
        deleted = false,
        availability = SavedSpotAvailability.AUTHOR_PRIVATE,
        isUserGenerated = true,
    )

    private fun setContent(
        state: ArchiveLoadState,
        selectedTab: ArchiveTab = ArchiveTab.SavedSpots,
        mySpotState: LoadState<List<MySpot>> = LoadState.Idle,
        onCellClick: (Long) -> Unit = {},
        onBookmarkTap: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreenContent(
                    state = state,
                    selectedTab = selectedTab,
                    archiveName = "나의 보관함",
                    mySpotState = mySpotState,
                    onCellClick = onCellClick,
                    onBookmarkTap = onBookmarkTap,
                )
            }
        }
    }

    @Test
    fun archive_loading_renders_placeholder() {
        setContent(state = ArchiveLoadState.Loading)

        composeRule.onNodeWithTag("archive-loading").assertIsDisplayed()
    }

    @Test
    fun archive_failed_renders_message() {
        setContent(state = ArchiveLoadState.Failed("네트워크 오류"))

        composeRule.onNodeWithTag("archive-failed").assertIsDisplayed()
        composeRule.onNodeWithText("문제가 발생했어요.\n네트워크 오류").assertIsDisplayed()
    }

    @Test
    fun my_tab_renders_five_status_policy() {
        setContent(
            state = ArchiveLoadState.Empty,
            selectedTab = ArchiveTab.MySpots,
            mySpotState = LoadState.Loaded(
                listOf(
                    mySpot(1L, MySpotStatus.PENDING),
                    mySpot(2L, MySpotStatus.RE_REVIEW_PENDING),
                    mySpot(3L, MySpotStatus.REJECTED),
                    mySpot(4L, MySpotStatus.DRAFT),
                    mySpot(5L, MySpotStatus.PUBLISHED),
                ),
            ),
        )

        // 검수 중 배지 2개(최초/재검수)는 같은 태그를 쓰므로 onAllNodesWithTag 로 센다.
        composeRule.onAllNodesWithTag("archive-my-badge-in-review", useUnmergedTree = true)
            .assertCountEquals(2)
        composeRule.onNodeWithTag("archive-my-badge-rejected", useUnmergedTree = true)
            .assertTextEquals("오픈 반려")
        composeRule.onNodeWithTag("archive-my-badge-public", useUnmergedTree = true)
            .assertTextEquals("공개")
        // 오픈 신청 전 DRAFT 는 배지가 없다.
        composeRule.onAllNodesWithTag("archive-my-badge-private", useUnmergedTree = true)
            .assertCountEquals(0)
        (1L..5L).forEach { id ->
            composeRule.onNodeWithTag("archive-my-cell-$id").assertExists()
        }
    }

    @Test
    fun my_tab_renders_private_badge_for_unpublished_spot() {
        setContent(
            state = ArchiveLoadState.Empty,
            selectedTab = ArchiveTab.MySpots,
            mySpotState = LoadState.Loaded(
                listOf(mySpot(1L, MySpotStatus.DRAFT, wasPublished = true)),
            ),
        )

        composeRule.onNodeWithTag("archive-my-badge-private", useUnmergedTree = true)
            .assertTextEquals("비공개")
    }

    @Test
    fun my_tab_shows_like_count_only_when_public_or_private() {
        setContent(
            state = ArchiveLoadState.Empty,
            selectedTab = ArchiveTab.MySpots,
            mySpotState = LoadState.Loaded(
                listOf(
                    mySpot(1L, MySpotStatus.PENDING),
                    mySpot(2L, MySpotStatus.REJECTED),
                    mySpot(3L, MySpotStatus.DRAFT),
                    mySpot(4L, MySpotStatus.PUBLISHED),
                    mySpot(5L, MySpotStatus.DRAFT, wasPublished = true),
                ),
            ),
        )

        // 공개·비공개 2건만 추천 수를 단다.
        composeRule.onAllNodesWithText("추천 34", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun private_saved_spot_opens_delete_modal() {
        var cellClickCount = 0
        setContent(
            state = ArchiveLoadState.Loaded(
                items = listOf(privateSavedSpot()),
                hasNext = false,
            ),
            onCellClick = { cellClickCount += 1 },
        )

        composeRule.onNodeWithText("등록한 유저가\n비공개로 전환하였어요").assertIsDisplayed()
        composeRule.onNodeWithTag("archive-private-$PRIVATE_SPOT_ID").performClick()

        composeRule.onNodeWithTag("archive-private-modal").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, cellClickCount) }
    }

    @Test
    fun private_delete_dialog_can_be_cancelled() {
        setContent(
            state = ArchiveLoadState.Loaded(items = listOf(privateSavedSpot()), hasNext = false),
        )
        composeRule.onNodeWithTag("archive-private-$PRIVATE_SPOT_ID").performClick()

        composeRule.onNodeWithText("저장 목록에서 삭제할까요?").assertIsDisplayed()
        composeRule.onNodeWithTag("archive-private-delete-cancel").performClick()

        composeRule.onNodeWithTag("archive-private-modal").assertDoesNotExist()
        // 취소해도 셀은 그대로 남는다.
        composeRule.onNodeWithTag("archive-private-$PRIVATE_SPOT_ID").assertExists()
    }

    @Test
    fun private_delete_confirms_once() {
        val archiveService = mockk<ArchiveService>()
        val bookmarkService = mockk<BookmarkService>()
        val authService = mockk<AuthService>()
        val locationService = mockk<LocationService>()
        val mySpotService = mockk<MySpotService>(relaxed = true)
        coEvery { authService.isLoggedIn() } returns true
        coEvery { locationService.currentLocation() } returns null
        coEvery { archiveService.fetch() } returns Archive("나의 보관함", null)
        coEvery { bookmarkService.savedSpots(0, null) } returns SavedSpotPage(
            items = listOf(privateSavedSpot()),
            page = 0,
            hasNext = false,
        )
        coEvery { bookmarkService.remove(PRIVATE_SPOT_ID.toString()) } returns 0L
        val viewModel = ArchiveViewModel(
            archiveService = archiveService,
            bookmarkService = bookmarkService,
            authService = authService,
            locationService = locationService,
            mySpotService = mySpotService,
        )
        var detailOpenCount = 0
        composeRule.setContent {
            PickflowTheme {
                ArchiveScreen(
                    onOpenSpotDetail = { detailOpenCount += 1 },
                    onOpenMySpot = {},
                    onRequireLogin = {},
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("archive-private-$PRIVATE_SPOT_ID")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("archive-private-$PRIVATE_SPOT_ID").performClick()

        composeRule.onNodeWithTag("archive-private-delete-confirm").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("archive-private-modal").assertDoesNotExist()
        composeRule.onNodeWithTag("archive-private-$PRIVATE_SPOT_ID").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, detailOpenCount) }
        coVerify(exactly = 1) { bookmarkService.remove(PRIVATE_SPOT_ID.toString()) }
    }

    private companion object {
        const val PRIVATE_SPOT_ID = 41L
    }
}
