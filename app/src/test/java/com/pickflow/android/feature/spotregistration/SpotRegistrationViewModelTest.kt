package com.pickflow.android.feature.spotregistration

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.CreateMySpotResult
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.LocalTime
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
class SpotRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mySpotService: MySpotService
    private lateinit var locationService: LocationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mySpotService = mockk()
        locationService = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SpotRegistrationViewModel(mySpotService, locationService)

    private fun address() = AddressSuggestion(
        name = "한강공원",
        fullAddress = "서울 동작구 한강대로",
        latitude = 37.5,
        longitude = 127.0,
    )

    private fun image() = ImagePayload(
        bytes = byteArrayOf(1, 2, 3),
        mimeType = "image/jpeg",
        filename = "spot.jpg",
    )

    /** 필수값(사진/이름/주소/테마/날짜/시간)을 모두 채운다. */
    private fun SpotRegistrationViewModel.fillRequired() {
        setImagePayload(image(), previewUri = null)
        setSpotName("Cafe")
        applyAddressSelection(address())
        toggleTheme(SpotTheme.YUNSEUL)
        setCapturedDate(LocalDate.of(2026, 5, 21))
        setCapturedTime(LocalTime.of(14, 30))
    }

    @Test
    fun `isRegisterEnabled false until all required fields set`() = runTest(testDispatcher) {
        val vm = vm()
        vm.isRegisterEnabled.test {
            assertFalse(awaitItem())
            vm.fillRequired()
            advanceUntilIdle()
            // 마지막 필수값 입력 후 true 로 전환.
            var enabled = false
            while (!enabled) { enabled = awaitItem() }
            assertTrue(enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSpotName clamps to 20 chars and comment to 50`() {
        val vm = vm()
        vm.setSpotName("a".repeat(40))
        assertEquals(SpotRegistrationViewModel.MAX_NAME_LENGTH, vm.spotName.value.length)
        vm.setComment("b".repeat(80))
        assertEquals(SpotRegistrationViewModel.MAX_COMMENT_LENGTH, vm.comment.value.length)
    }

    @Test
    fun `setCapturedDate clamps future date to today`() {
        val vm = vm()
        vm.setCapturedDate(LocalDate.now().plusDays(3))
        assertEquals(LocalDate.now(), vm.capturedDate.value)
    }

    @Test
    fun `submit fails fast when invalid`() = runTest(testDispatcher) {
        val vm = vm()
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.submission.value is LoadState.Failed)
    }

    @Test
    fun `submit success emits Loaded with created result`() = runTest(testDispatcher) {
        val captured = slot<SpotDraft>()
        coEvery { mySpotService.create(capture(captured), any()) } returns CreateMySpotResult(
            spotId = 42L,
            status = MySpotStatus.PENDING,
            imageUrl = "https://s3/42.jpg",
        )

        val vm = vm()
        vm.fillRequired()
        vm.submit()
        advanceUntilIdle()

        val state = vm.submission.value as LoadState.Loaded
        assertEquals(42L, state.value.spotId)
        assertEquals(MySpotStatus.PENDING, state.value.status)

        // 드래프트는 서버 포맷(yyyy-MM-dd / HH:mm)으로 직렬화된다.
        assertEquals("Cafe", captured.captured.name)
        assertEquals(SpotTheme.YUNSEUL, captured.captured.theme)
        assertEquals("2026-05-21", captured.captured.capturedDate)
        assertEquals("14:30", captured.captured.capturedTime)
        coVerify(exactly = 1) { mySpotService.create(any(), any()) }
    }
}
