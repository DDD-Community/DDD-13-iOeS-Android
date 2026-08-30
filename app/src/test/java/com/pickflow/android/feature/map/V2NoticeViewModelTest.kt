package com.pickflow.android.feature.map

import com.pickflow.android.core.services.protocols.V2NoticeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * V2 안내 팝업은 플래그가 비어 있을 때만 뜨고, 확인 즉시 플래그가 심긴다.
 * Dev Mode 스위치가 플래그를 되돌리면 살아 있는 화면에도 곧바로 다시 떠야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class V2NoticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    /** 실제 앱에서 Hilt 가 @Singleton 으로 하나만 주입하는 것과 같은 조건. */
    private class FakeV2NoticeStore(initial: Boolean = false) : V2NoticeStore {
        private val _seen = MutableStateFlow(initial)
        override val seen: StateFlow<Boolean> = _seen.asStateFlow()
        override fun setSeen(seen: Boolean) {
            _seen.value = seen
        }
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `플래그가 없으면 최초 1회 노출된다`() = runTest(testDispatcher) {
        val viewModel = V2NoticeViewModel(FakeV2NoticeStore(initial = false))
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `이미 본 기기에서는 뜨지 않는다`() = runTest(testDispatcher) {
        val viewModel = V2NoticeViewModel(FakeV2NoticeStore(initial = true))
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `확인하면 닫히고 플래그가 심긴다`() = runTest(testDispatcher) {
        val store = FakeV2NoticeStore(initial = false)
        val viewModel = V2NoticeViewModel(store)
        advanceUntilIdle()

        viewModel.confirm()
        advanceUntilIdle()

        assertTrue(store.seen.value)
        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `Dev Mode 스위치로 플래그를 되돌리면 화면을 다시 열지 않아도 뜬다`() = runTest(testDispatcher) {
        val store = FakeV2NoticeStore(initial = true)
        val viewModel = V2NoticeViewModel(store)
        advanceUntilIdle()

        // Dev Mode 의 setV2NoticeEnabled(true) 와 같은 조작.
        store.setSeen(false)
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }
}
