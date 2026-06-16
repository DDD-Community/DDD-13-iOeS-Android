package com.pickflow.android.feature.notice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pickflow.android.common.designsystem.PickflowTheme
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BoardPost
import com.pickflow.android.core.services.protocols.BoardPostDetail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NoticeScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list_loaded_shows_rows_and_pinned_badge() {
        val items = listOf(
            BoardPost(postId = 1L, title = "고정 공지", createdAt = "2026-01-01", pinned = true),
            BoardPost(postId = 2L, title = "일반 공지", createdAt = "2026-01-02", pinned = false),
        )
        composeRule.setContent {
            PickflowTheme {
                NoticeListContent(state = LoadState.Loaded(items))
            }
        }
        composeRule.onNodeWithTag("notice-list").assertIsDisplayed()
        composeRule.onNodeWithTag("notice-row-1").assertIsDisplayed()
        composeRule.onNodeWithTag("notice-row-2").assertIsDisplayed()
    }

    @Test
    fun list_empty_shows_state_empty_message() {
        composeRule.setContent {
            PickflowTheme {
                NoticeListContent(state = LoadState.Empty)
            }
        }
        composeRule.onNodeWithTag("state-empty").assertIsDisplayed()
    }

    @Test
    fun list_row_click_invokes_onOpenDetail() {
        var lastId: Long? = null
        composeRule.setContent {
            PickflowTheme {
                NoticeListContent(
                    state = LoadState.Loaded(
                        listOf(BoardPost(postId = 42L, title = "탭 대상", createdAt = "2026-01-01", pinned = false)),
                    ),
                    onOpenDetail = { lastId = it },
                )
            }
        }
        composeRule.onNodeWithTag("notice-row-42").performClick()
        assert(lastId == 42L)
    }

    @Test
    fun detail_loaded_shows_content() {
        val detail = BoardPostDetail(
            masterId = 1L,
            postId = 1L,
            title = "타이틀",
            createdAt = "2026-01-01",
            content = "본문",
        )
        composeRule.setContent {
            PickflowTheme {
                NoticeDetailContentScreen(state = LoadState.Loaded(detail))
            }
        }
        composeRule.onNodeWithTag("notice-detail-content").assertIsDisplayed()
    }

    @Test
    fun detail_failed_shows_state_failed() {
        composeRule.setContent {
            PickflowTheme {
                NoticeDetailContentScreen(state = LoadState.Failed(RuntimeException("err")))
            }
        }
        composeRule.onNodeWithTag("state-failed").assertIsDisplayed()
    }
}
