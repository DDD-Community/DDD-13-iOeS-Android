package com.pickflow.android.feature.notice

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BoardPost
import com.pickflow.android.core.services.protocols.BoardPostPage
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
class NoticeListViewModelTest {

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

    private fun post(id: Long, pinned: Boolean = false) = BoardPost(
        postId = id,
        title = "공지 $id",
        createdAt = "2026-01-01",
        pinned = pinned,
    )

    @Test
    fun `refresh emits Loaded with page 0 items`() = runTest(testDispatcher) {
        coEvery { boardService.posts(any(), 0) } returns BoardPostPage(
            items = listOf(post(1, pinned = true), post(2)),
            page = 0,
            hasNext = true,
        )
        val vm = NoticeListViewModel(boardService)
        vm.refresh(); advanceUntilIdle()
        val s = vm.posts.value
        assertTrue(s is LoadState.Loaded && s.value.map { it.postId } == listOf(1L, 2L))
    }

    @Test
    fun `loadNextPage accumulates and stops when hasNext false`() = runTest(testDispatcher) {
        coEvery { boardService.posts(any(), 0) } returns BoardPostPage(listOf(post(1)), 0, true)
        coEvery { boardService.posts(any(), 1) } returns BoardPostPage(listOf(post(2)), 1, false)
        val vm = NoticeListViewModel(boardService)
        vm.refresh(); advanceUntilIdle()
        vm.loadNextPage(); advanceUntilIdle()
        val loaded = vm.posts.value as LoadState.Loaded
        assertEquals(2, loaded.value.size)
        vm.loadNextPage(); advanceUntilIdle()
        assertEquals(2, (vm.posts.value as LoadState.Loaded).value.size)
        coVerify(exactly = 1) { boardService.posts(any(), 1) }
    }

    @Test
    fun `empty result emits Empty`() = runTest(testDispatcher) {
        coEvery { boardService.posts(any(), 0) } returns BoardPostPage(emptyList(), 0, false)
        val vm = NoticeListViewModel(boardService)
        vm.refresh(); advanceUntilIdle()
        assertEquals(LoadState.Empty, vm.posts.value)
    }

    @Test
    fun `failure emits Failed`() = runTest(testDispatcher) {
        val boom = RuntimeException("nope")
        coEvery { boardService.posts(any(), any()) } throws boom
        val vm = NoticeListViewModel(boardService)
        vm.refresh(); advanceUntilIdle()
        val s = vm.posts.value
        assertTrue(s is LoadState.Failed && s.error === boom)
    }
}
