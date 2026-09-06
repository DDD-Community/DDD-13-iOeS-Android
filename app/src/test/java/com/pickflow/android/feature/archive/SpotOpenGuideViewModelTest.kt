package com.pickflow.android.feature.archive

import com.pickflow.android.core.services.impl.FakeNewFeatureGuide
import com.pickflow.android.core.services.protocols.SpotOpenGuideStore
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

/**
 * 노출 = `원격 활성 && 아직 안 봤음(계정별) && 로그인됨`.
 *
 * V2 안내와 정반대로 **로그인 게이팅이 맞는** 건이다(PV-79 §6) — 안내 대상이 "내 스팟" 이라
 * 비로그인에는 띄울 것이 없다. 게이트는 [SpotOpenGuideStore.hasSeen] 의 null 로 걸린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpotOpenGuideViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    /** null = 비로그인. [PrefsSpotOpenGuideStore] 의 계정별 동작은 그쪽 테스트가 본다. */
    private class FakeStore(var seen: Boolean?) : SpotOpenGuideStore {
        override suspend fun hasSeen(): Boolean? = seen
        override suspend fun markSeen() {
            if (seen != null) seen = true
        }
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `원격이 켜져 있고 로그인했고 아직 안 봤으면 뜬다`() = runTest(testDispatcher) {
        val viewModel = viewModel(FakeStore(seen = false), FakeNewFeatureGuide())
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `비로그인이면 원격이 켜져 있어도 뜨지 않는다`() = runTest(testDispatcher) {
        val viewModel = viewModel(FakeStore(seen = null), FakeNewFeatureGuide())
        advanceUntilIdle()

        assertFalse(viewModel.visible.value, "안내 대상이 내 스팟이라 비로그인에는 띄울 게 없다")
    }

    @Test
    fun `원격이 꺼져 있으면 아직 안 봤어도 뜨지 않는다`() = runTest(testDispatcher) {
        val viewModel = viewModel(FakeStore(seen = false), FakeNewFeatureGuide(active = false))
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `이미 본 계정에서는 뜨지 않는다`() = runTest(testDispatcher) {
        val viewModel = viewModel(FakeStore(seen = true), FakeNewFeatureGuide())
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `확인하면 닫히고 계정에 봤음이 남는다`() = runTest(testDispatcher) {
        val store = FakeStore(seen = false)
        val viewModel = viewModel(store, FakeNewFeatureGuide())
        advanceUntilIdle()

        viewModel.confirm()
        advanceUntilIdle()

        assertTrue(store.seen == true)
        assertFalse(viewModel.visible.value)
    }

    @Test
    fun `fetch 로 원격이 켜지면 화면을 다시 열지 않아도 뜬다`() = runTest(testDispatcher) {
        val guide = FakeNewFeatureGuide(active = false, activeAfterRefresh = true)
        val viewModel = viewModel(FakeStore(seen = false), guide)
        advanceUntilIdle()

        assertFalse(viewModel.visible.value)

        viewModel.onAppear()
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    @Test
    fun `화면에 머문 채 로그인해도 재평가된다`() = runTest(testDispatcher) {
        val store = FakeStore(seen = null)
        val viewModel = viewModel(store, FakeNewFeatureGuide())
        advanceUntilIdle()
        assertFalse(viewModel.visible.value)

        // 보관함이 로그인 진입점이라 같은 화면에서 계정이 생긴다.
        store.seen = false
        viewModel.onAppear()
        advanceUntilIdle()

        assertTrue(viewModel.visible.value)
    }

    private fun viewModel(store: SpotOpenGuideStore, guide: FakeNewFeatureGuide) =
        SpotOpenGuideViewModel(guide, store)
}
