package com.pickflow.android.feature.devmode

import app.cash.turbine.test
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.network.ApiEnvironmentInterceptor
import com.pickflow.android.core.services.protocols.ApiEnvironment
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.DevSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeDevSettings(
    environment: ApiEnvironment = ApiEnvironment.DEFAULT,
    badge: Boolean = false,
) : DevSettings {
    private val _apiEnvironment = MutableStateFlow(environment)
    private val _badgeEnabled = MutableStateFlow(badge)
    private val _touchIndicatorEnabled = MutableStateFlow(false)
    override val apiEnvironment: StateFlow<ApiEnvironment> = _apiEnvironment
    override val badgeEnabled: StateFlow<Boolean> = _badgeEnabled
    override val touchIndicatorEnabled: StateFlow<Boolean> = _touchIndicatorEnabled
    override fun setApiEnvironment(environment: ApiEnvironment) {
        _apiEnvironment.value = environment
    }
    override fun setBadgeEnabled(enabled: Boolean) {
        _badgeEnabled.value = enabled
    }
    override fun setTouchIndicatorEnabled(enabled: Boolean) {
        _touchIndicatorEnabled.value = enabled
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DevModeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authService = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(settings: DevSettings) = DevModeViewModel(settings, authService)

    @Test
    fun `기본 환경은 빌드 타입이 보던 서버이고 선택하면 즉시 반영된다`() = runTest {
        // 저장된 값이 없으면 debug=dev, release=prod 로 기존 동작이 유지돼야 한다.
        assertEquals(
            BuildConfig.PICKFLOW_API_BASE_URL,
            ApiEnvironment.DEFAULT.baseUrl,
        )

        val vm = vm(FakeDevSettings())

        val other = ApiEnvironment.entries.first { it != ApiEnvironment.DEFAULT }
        vm.apiEnvironment.test {
            assertEquals(ApiEnvironment.DEFAULT, awaitItem())
            // 고르기만 해서는 안 바뀌고, 확인을 받아야 적용된다.
            vm.selectEnvironment(other)
            expectNoEvents()
            vm.confirmEnvironmentChange()
            advanceUntilIdle()
            assertEquals(other, awaitItem())
        }
    }

    @Test
    fun `배지 토글은 저장소 값을 따라간다`() = runTest {
        val vm = vm(FakeDevSettings())

        vm.badgeEnabled.test {
            assertEquals(false, awaitItem())
            vm.setBadgeEnabled(true)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `환경 전환을 확인하면 이전 서버 세션을 정리한 뒤 적용한다`() = runTest {
        val settings = FakeDevSettings()
        val target = ApiEnvironment.entries.first { it != ApiEnvironment.DEFAULT }
        coEvery { authService.logout() } returns Unit
        val vm = vm(settings)

        vm.selectEnvironment(target)
        assertEquals(target, vm.pendingEnvironment.value)

        vm.confirmEnvironmentChange()
        advanceUntilIdle()

        coVerify(exactly = 1) { authService.logout() }
        assertEquals(target, settings.apiEnvironment.value)
        assertEquals(null, vm.pendingEnvironment.value)
    }

    @Test
    fun `로그아웃이 실패해도 환경은 적용된다`() = runTest {
        val settings = FakeDevSettings()
        val target = ApiEnvironment.entries.first { it != ApiEnvironment.DEFAULT }
        coEvery { authService.logout() } throws RuntimeException("network")
        val vm = vm(settings)

        vm.selectEnvironment(target)
        vm.confirmEnvironmentChange()
        advanceUntilIdle()

        assertEquals(target, settings.apiEnvironment.value)
    }

    @Test
    fun `같은 환경을 다시 고르면 다이얼로그를 띄우지 않는다`() = runTest {
        val vm = vm(FakeDevSettings())

        vm.selectEnvironment(ApiEnvironment.DEFAULT)

        assertEquals(null, vm.pendingEnvironment.value)
    }

    // --- 인터셉터: 실제 서버 전환이 일어나는 지점이라 함께 검증한다. ---

    private fun intercepted(requestUrl: String, settings: DevSettings): String {
        val request = Request.Builder().url(requestUrl).build()
        val sent = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(capture(sent)) } answers {
            Response.Builder()
                .request(sent.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody(null))
                .build()
        }
        ApiEnvironmentInterceptor(settings).intercept(chain)
        return sent.captured.url.toString()
    }

    @Test
    fun `dev 를 고르면 픽플로우 요청 host 만 개발 서버로 바뀐다`() {
        val settings = FakeDevSettings(environment = ApiEnvironment.DEV)
        val prodRequest = "${ApiEnvironment.PROD.baseUrl}spots?page=1"

        assertEquals(
            "${ApiEnvironment.DEV.baseUrl}spots?page=1",
            intercepted(prodRequest, settings),
        )
    }

    @Test
    fun `픽플로우 API 가 아닌 요청은 건드리지 않는다`() {
        val settings = FakeDevSettings(environment = ApiEnvironment.DEV)
        val kakao = "https://dapi.kakao.com/v2/local/geo/coord2address.json"

        assertEquals(kakao, intercepted(kakao, settings))
    }

    @Test
    fun `이미 선택된 환경이면 URL 을 그대로 통과시킨다`() {
        val settings = FakeDevSettings(environment = ApiEnvironment.PROD)
        val prodRequest = "${ApiEnvironment.PROD.baseUrl}users/me"

        assertEquals(prodRequest, intercepted(prodRequest, settings))
    }
}
