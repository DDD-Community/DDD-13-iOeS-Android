package com.pickflow.android.feature.onboarding

import app.cash.turbine.test
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
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
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: OnboardingCompletionStore

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = mockk(relaxed = true)
        coEvery { store.isCompleted() } returns false
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `next advances pageIndex within bounds`() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(store)
        vm.pageIndex.test {
            assertEquals(0, awaitItem())
            vm.next()
            assertEquals(1, awaitItem())
            vm.next()
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `next on last page finishes and marks completed`() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(store)
        repeat(vm.pages.size) { vm.next() } // 마지막 페이지에서 한 번 더 -> finish
        advanceUntilIdle()
        assertTrue(vm.completed.value)
        coVerify(exactly = 1) { store.setCompleted(true) }
    }

    @Test
    fun `onboarding has four pages matching iOS`() {
        val vm = OnboardingViewModel(store)
        assertEquals(4, vm.pages.size)
    }

    @Test
    fun `previous never goes below zero`() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(store)
        vm.previous()
        assertEquals(0, vm.pageIndex.value)
    }

    @Test
    fun `setPage reflects swipe and clamps within bounds`() {
        val vm = OnboardingViewModel(store)
        vm.setPage(2)
        assertEquals(2, vm.pageIndex.value)
        vm.setPage(99)
        assertEquals(vm.pages.lastIndex, vm.pageIndex.value)
        vm.setPage(-5)
        assertEquals(0, vm.pageIndex.value)
    }
}
