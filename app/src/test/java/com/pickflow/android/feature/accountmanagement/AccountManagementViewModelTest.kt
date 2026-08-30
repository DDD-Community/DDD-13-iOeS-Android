package com.pickflow.android.feature.accountmanagement

import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.GuestEntryStore
import com.pickflow.android.core.services.protocols.UserService
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun logoutWithGuestHistory(entered: Boolean): AccountManagementViewModel {
        val guestEntryStore = mockk<GuestEntryStore>()
        coEvery { guestEntryStore.hasEntered() } returns entered
        return AccountManagementViewModel(
            mockk<AuthService>(relaxed = true),
            mockk<UserService>(relaxed = true),
            guestEntryStore,
        ).apply { logout() }
    }

    @Test
    fun `비회원 탐색 이력이 있으면 로그아웃 후 탐색 탭으로 돌아간다`() = runTest(testDispatcher) {
        val vm = logoutWithGuestHistory(entered = true)
        advanceUntilIdle()
        assertTrue(vm.signedOut.value)
        assertTrue(vm.keepBrowsingAfterSignOut.value)
    }

    @Test
    fun `이력이 없으면 로그아웃 후 로그인 화면으로 간다`() = runTest(testDispatcher) {
        val vm = logoutWithGuestHistory(entered = false)
        advanceUntilIdle()
        assertTrue(vm.signedOut.value)
        assertFalse(vm.keepBrowsingAfterSignOut.value)
    }
}
