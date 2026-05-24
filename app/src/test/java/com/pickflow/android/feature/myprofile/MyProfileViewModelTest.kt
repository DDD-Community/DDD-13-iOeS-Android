package com.pickflow.android.feature.myprofile

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.UserService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUserName emits Loading then Loaded`() = runTest(testDispatcher) {
        val service = mockk<UserService>()
        coEvery { service.fetchUserName() } returns "Alice"
        val viewModel = MyProfileViewModel(service)

        viewModel.userName.test {
            assertEquals(LoadState.Idle, awaitItem())
            viewModel.loadUserName()
            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded("Alice"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadUserName emits Empty for blank name`() = runTest(testDispatcher) {
        val service = mockk<UserService>()
        coEvery { service.fetchUserName() } returns ""
        val viewModel = MyProfileViewModel(service)

        viewModel.userName.test {
            assertEquals(LoadState.Idle, awaitItem())
            viewModel.loadUserName()
            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadUserName emits Failed on exception`() = runTest(testDispatcher) {
        val service = mockk<UserService>()
        val error = RuntimeException("boom")
        coEvery { service.fetchUserName() } throws error
        val viewModel = MyProfileViewModel(service)

        viewModel.userName.test {
            assertEquals(LoadState.Idle, awaitItem())
            viewModel.loadUserName()
            assertEquals(LoadState.Loading, awaitItem())
            val failed = awaitItem()
            assertTrue(failed is LoadState.Failed && failed.error === error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
