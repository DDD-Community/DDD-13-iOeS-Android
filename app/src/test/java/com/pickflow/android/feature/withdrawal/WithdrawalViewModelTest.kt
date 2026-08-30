package com.pickflow.android.feature.withdrawal

import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.GuestEntryStore
import com.pickflow.android.feature.withdrawal.model.WithdrawalReason
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WithdrawalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun withdrawWithGuestHistory(entered: Boolean): WithdrawalViewModel {
        val guestEntryStore = mockk<GuestEntryStore>()
        coEvery { guestEntryStore.hasEntered() } returns entered
        return WithdrawalViewModel(mockk<AuthService>(relaxed = true), guestEntryStore).apply {
            selectReason(WithdrawalReason.RarelyUsed)
            toggleAgreement()
            submitWithdrawal()
        }
    }

    @Test
    fun `비회원 탐색 이력이 있으면 탈퇴 후 탐색 탭으로 돌아간다`() = runTest(testDispatcher) {
        val vm = withdrawWithGuestHistory(entered = true)
        advanceUntilIdle()
        assertEquals(WithdrawalViewModel.Step.Done, vm.step.value)
        assertTrue(vm.keepBrowsingAfterWithdrawal.value)
    }

    @Test
    fun `이력이 없으면 탈퇴 후 로그인 화면으로 간다`() = runTest(testDispatcher) {
        val vm = withdrawWithGuestHistory(entered = false)
        advanceUntilIdle()
        assertEquals(WithdrawalViewModel.Step.Done, vm.step.value)
        assertFalse(vm.keepBrowsingAfterWithdrawal.value)
    }
}
