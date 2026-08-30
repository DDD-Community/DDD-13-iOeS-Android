package com.pickflow.android.feature.map

import com.pickflow.android.core.services.protocols.V2NoticeStore
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** V2 안내 팝업은 플래그가 비어 있을 때만 뜨고, 확인 즉시 플래그가 심긴다. */
@OptIn(ExperimentalCoroutinesApi::class)
class V2NoticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: V2NoticeStore

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `플래그가 없으면 최초 1회 노출된다`() = runTest(testDispatcher) {
        coEvery { store.isSeen() } returns false

        val viewModel = V2NoticeViewModel(store)
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `이미 본 기기에서는 뜨지 않는다`() = runTest(testDispatcher) {
        coEvery { store.isSeen() } returns true

        val viewModel = V2NoticeViewModel(store)
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `확인하면 닫히고 플래그가 심긴다`() = runTest(testDispatcher) {
        coEvery { store.isSeen() } returns false
        val viewModel = V2NoticeViewModel(store)
        advanceUntilIdle()

        viewModel.confirm()
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
        coVerify(exactly = 1) { store.markSeen() }
    }
}
