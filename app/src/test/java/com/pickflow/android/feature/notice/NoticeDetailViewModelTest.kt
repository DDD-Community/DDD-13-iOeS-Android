package com.pickflow.android.feature.notice

import androidx.lifecycle.SavedStateHandle
import com.pickflow.android.BuildConfig
import com.pickflow.android.app.navigation.PickflowRoute
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BoardPostDetail
import com.pickflow.android.core.services.protocols.BoardService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoticeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var boardService: BoardService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        boardService = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun handle(postId: Long) =
        SavedStateHandle(mapOf(PickflowRoute.ARG_NOTICE_POST_ID to postId))

    @Test
    fun `load emits Loaded with detail`() = runTest(testDispatcher) {
        val detail = BoardPostDetail(
            masterId = BuildConfig.NOTICE_BOARD_MASTER_ID,
            postId = 42L,
            title = "공지 제목",
            createdAt = "2026-01-01",
            content = "본문",
        )
        coEvery { boardService.detail(BuildConfig.NOTICE_BOARD_MASTER_ID, 42L) } returns detail
        val vm = NoticeDetailViewModel(boardService, handle(42L))
        vm.load(); advanceUntilIdle()
        assertEquals(LoadState.Loaded(detail), vm.post.value)
        coVerify(exactly = 1) { boardService.detail(BuildConfig.NOTICE_BOARD_MASTER_ID, 42L) }
    }

    @Test
    fun `load emits Failed on error`() = runTest(testDispatcher) {
        val boom = RuntimeException("net")
        coEvery { boardService.detail(any(), any()) } throws boom
        val vm = NoticeDetailViewModel(boardService, handle(1L))
        vm.load(); advanceUntilIdle()
        val s = vm.post.value
        assertTrue(s is LoadState.Failed && s.error === boom)
    }
}
