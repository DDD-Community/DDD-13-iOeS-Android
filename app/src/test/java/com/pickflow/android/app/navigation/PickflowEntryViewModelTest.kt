package com.pickflow.android.app.navigation

import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.GuestEntryStore
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import io.mockk.coEvery
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PickflowEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `온보딩 전이면 이력과 무관하게 온보딩부터`() = runTest(testDispatcher) {
        val vm = PickflowEntryViewModel(
            mockk { coEvery { isCompleted() } returns false },
            mockk { coEvery { isLoggedIn() } returns true },
            mockk { coEvery { hasEntered() } returns true },
        )
        advanceUntilIdle()
        assertEquals(PickflowRoute.ONBOARDING, vm.startDestination.value)
    }

    @Test
    fun `온보딩만 마쳤고 이력이 없으면 로그인`() = runTest(testDispatcher) {
        val vm = PickflowEntryViewModel(
            mockk { coEvery { isCompleted() } returns true },
            mockk { coEvery { isLoggedIn() } returns false },
            mockk { coEvery { hasEntered() } returns false },
        )
        advanceUntilIdle()
        assertEquals(PickflowRoute.LOGIN, vm.startDestination.value)
    }

    @Test
    fun `비회원 진입 이력이 있으면 로그인 화면을 건너뛰고 탐색 탭으로`() = runTest(testDispatcher) {
        val vm = PickflowEntryViewModel(
            mockk { coEvery { isCompleted() } returns true },
            mockk { coEvery { isLoggedIn() } returns false },
            mockk { coEvery { hasEntered() } returns true },
        )
        advanceUntilIdle()
        assertEquals(PickflowRoute.HOME, vm.startDestination.value)
    }

    @Test
    fun `로그인 상태면 이력과 무관하게 탐색 탭으로`() = runTest(testDispatcher) {
        val vm = PickflowEntryViewModel(
            mockk { coEvery { isCompleted() } returns true },
            mockk { coEvery { isLoggedIn() } returns true },
            mockk { coEvery { hasEntered() } returns false },
        )
        advanceUntilIdle()
        assertEquals(PickflowRoute.HOME, vm.startDestination.value)
    }
}
