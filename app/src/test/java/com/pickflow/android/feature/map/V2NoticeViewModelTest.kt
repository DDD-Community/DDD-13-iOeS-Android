package com.pickflow.android.feature.map

import com.pickflow.android.core.services.impl.FakeNewFeatureGuide
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
 * V2 안내 팝업 = `원격이 켜져 있음(Remote Config) && 아직 안 봤음(로컬 플래그)`.
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
    fun `원격이 켜져 있고 플래그가 없으면 노출된다`() = runTest(testDispatcher) {
        val viewModel = V2NoticeViewModel(FakeV2NoticeStore(initial = false), FakeNewFeatureGuide())
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `원격이 꺼져 있으면 아직 안 봤어도 뜨지 않는다`() = runTest(testDispatcher) {
        val viewModel = V2NoticeViewModel(
            FakeV2NoticeStore(initial = false),
            FakeNewFeatureGuide(active = false),
        )
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `fetch 로 원격이 켜지면 화면을 다시 열지 않아도 뜬다`() = runTest(testDispatcher) {
        val guide = FakeNewFeatureGuide(active = false, activeAfterRefresh = true)
        val viewModel = V2NoticeViewModel(FakeV2NoticeStore(initial = false), guide)

        assertFalse(viewModel.visible.value, "fetch 전에는 꺼져 있다")

        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `이미 본 기기에서는 원격이 켜져 있어도 뜨지 않는다`() = runTest(testDispatcher) {
        val viewModel = V2NoticeViewModel(FakeV2NoticeStore(initial = true), FakeNewFeatureGuide())
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `확인하면 닫히고 플래그가 심긴다`() = runTest(testDispatcher) {
        val store = FakeV2NoticeStore(initial = false)
        val viewModel = V2NoticeViewModel(store, FakeNewFeatureGuide())
        advanceUntilIdle()

        viewModel.confirm()
        advanceUntilIdle()

        assertTrue(store.seen.value)
        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `Dev Mode 스위치로 플래그를 되돌리면 화면을 다시 열지 않아도 뜬다`() = runTest(testDispatcher) {
        val store = FakeV2NoticeStore(initial = true)
        val viewModel = V2NoticeViewModel(store, FakeNewFeatureGuide())
        advanceUntilIdle()

        // Dev Mode 의 setV2NoticeEnabled(true) 와 같은 조작.
        store.setSeen(false)
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `화면 진입 시 원격 설정을 갱신한다`() = runTest(testDispatcher) {
        val guide = FakeNewFeatureGuide()
        V2NoticeViewModel(FakeV2NoticeStore(), guide)
        advanceUntilIdle()

        assertTrue(guide.refreshCount == 1)
    }

    /**
     * PV-79 §6 — iOS 는 이 평가를 로그인 분기 안에 넣어 게스트가 모달을 영영 못 받았다.
     * V2 안내가 알리는 햇살·야경 필터는 로그인과 무관한 기능이라 게스트도 대상이다.
     *
     * 이 ViewModel 은 [com.pickflow.android.core.services.protocols.AuthService] 를 아예 받지 않는다 —
     * 인증 의존성이 생기는 순간 이 테스트는 컴파일되지 않는다. 그게 이 테스트의 방어선이다.
     */
    @Test
    fun `비로그인 상태에서도 평가된다`() = runTest(testDispatcher) {
        val viewModel = V2NoticeViewModel(FakeV2NoticeStore(initial = false), FakeNewFeatureGuide())
        advanceUntilIdle()

        assertTrue(viewModel.visible.value, "로그인 정보가 아예 없어도 떠야 한다")
    }
}
