package com.pickflow.android.feature.login

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.GuestEntryStore
import com.pickflow.android.core.services.protocols.KakaoAuthProvider
import com.pickflow.android.core.services.protocols.KakaoAuthResult
import com.pickflow.android.core.services.protocols.AuthenticatedSession
import com.pickflow.android.core.services.protocols.SessionTokens
import com.pickflow.android.core.services.protocols.UserProfile
import com.pickflow.android.core.services.protocols.SocialAuthCredential
import com.pickflow.android.core.services.protocols.SocialLoginService
import com.pickflow.android.core.services.protocols.SocialProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var kakao: KakaoAuthProvider
    private lateinit var social: SocialLoginService
    private lateinit var auth: AuthService
    private lateinit var guestEntryStore: GuestEntryStore

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        kakao = mockk()
        social = mockk()
        auth = mockk(relaxed = true)
        guestEntryStore = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loginWithKakao emits Loaded session and forwards credential`() = runTest(testDispatcher) {
        coEvery { kakao.login() } returns KakaoAuthResult("k-access", "k-refresh")
        val captured = slot<SocialAuthCredential>()
        val session = AuthenticatedSession(
            tokens = SessionTokens("sess", "rf"),
            profile = UserProfile(
                userId = "u1",
                email = "kdy@example.com",
                nickname = "pickflower",
                profileImageUrl = null,
                provider = SocialProvider.KAKAO,
            ),
        )
        coEvery { social.loginWith(capture(captured)) } returns session

        val vm = LoginViewModel(kakao, social, auth, guestEntryStore)

        vm.session.test {
            assertEquals(LoadState.Idle, awaitItem())
            vm.loginWithKakao()
            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded(session), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(SocialProvider.KAKAO, captured.captured.provider)
        assertEquals("k-access", captured.captured.providerAccessToken)
        assertEquals("k-refresh", captured.captured.providerRefreshToken)
        coVerify(exactly = 1) { kakao.login() }
        coVerify(exactly = 1) { social.loginWith(any()) }
    }

    @Test
    fun `loginWithKakao emits Failed when kakao provider throws`() = runTest(testDispatcher) {
        val boom = IllegalStateException("kakao down")
        coEvery { kakao.login() } throws boom

        val vm = LoginViewModel(kakao, social, auth, guestEntryStore)

        vm.session.test {
            assertEquals(LoadState.Idle, awaitItem())
            vm.loginWithKakao()
            assertEquals(LoadState.Loading, awaitItem())
            val failed = awaitItem()
            assertTrue(failed is LoadState.Failed && failed.error === boom)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `비회원 시작은 이력을 저장한 뒤에 이동한다`() = runTest(testDispatcher) {
        // 순서가 뒤집히면 이동으로 로그인 화면이 사라지며 저장이 취소될 수 있다.
        val order = mutableListOf<String>()
        coEvery { guestEntryStore.setEntered(true) } coAnswers { order += "store" }

        LoginViewModel(kakao, social, auth, guestEntryStore).enterAsGuest { order += "nav" }
        advanceUntilIdle()

        assertEquals(listOf("store", "nav"), order)
    }
}
