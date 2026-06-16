package com.pickflow.android.feature.myprofile

import app.cash.turbine.test
import com.pickflow.android.BuildConfig
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.core.services.protocols.UserService
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
class MyProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userService: UserService
    private lateinit var authService: AuthService
    private lateinit var externalAppLauncher: ExternalAppLauncher

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userService = mockk()
        authService = mockk()
        externalAppLauncher = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = MyProfileViewModel(userService, authService, externalAppLauncher)

    private fun home(nickname: String = "테스트유저#1234") = MyPageHome(
        nickname = nickname,
        profileImageUrl = null,
        savedSpotCount = 0,
        recordedSpotCount = 0,
    )

    @Test
    fun `load skips myPage fetch when not logged in`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns false
        val viewModel = vm()
        viewModel.load()
        advanceUntilIdle()
        assertEquals(false, viewModel.loggedIn.value)
        assertEquals(LoadState.Idle, viewModel.myPage.value)
    }

    @Test
    fun `load emits Loaded MyPageHome when logged in`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        coEvery { userService.fetchMyPage() } returns home()
        val viewModel = vm()

        viewModel.myPage.test {
            assertEquals(LoadState.Idle, awaitItem())
            viewModel.load()
            assertEquals(LoadState.Loading, awaitItem())
            val loaded = awaitItem() as LoadState.Loaded<MyPageHome>
            assertEquals("테스트유저#1234", loaded.value.nickname)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(true, viewModel.loggedIn.value)
    }

    @Test
    fun `load emits Failed on exception`() = runTest(testDispatcher) {
        coEvery { authService.isLoggedIn() } returns true
        val error = RuntimeException("boom")
        coEvery { userService.fetchMyPage() } throws error
        val viewModel = vm()

        viewModel.myPage.test {
            assertEquals(LoadState.Idle, awaitItem())
            viewModel.load()
            assertEquals(LoadState.Loading, awaitItem())
            val failed = awaitItem()
            assertTrue(failed is LoadState.Failed && failed.error === error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openTerms launches Custom Tab with TERMS_URL`() = runTest(testDispatcher) {
        val viewModel = vm()
        viewModel.openTerms()
        advanceUntilIdle()
        coVerify(exactly = 1) { externalAppLauncher.openCustomTab(BuildConfig.TERMS_URL) }
    }

    @Test
    fun `openPrivacy launches Custom Tab with PRIVACY_URL`() = runTest(testDispatcher) {
        val viewModel = vm()
        viewModel.openPrivacy()
        advanceUntilIdle()
        coVerify(exactly = 1) { externalAppLauncher.openCustomTab(BuildConfig.PRIVACY_URL) }
    }
}
