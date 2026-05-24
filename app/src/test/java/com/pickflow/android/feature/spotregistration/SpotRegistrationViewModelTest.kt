package com.pickflow.android.feature.spotregistration

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spotService: SpotService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        spotService = mockk()
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `isValid returns false when fields missing`() {
        val vm = SpotRegistrationViewModel(spotService)
        assertFalse(vm.isValid())
        vm.setName("Cafe")
        vm.setAddress("Seoul")
        vm.setCoordinates(1.0, 2.0)
        assertFalse(vm.isValid()) // no captured date/time
        vm.setCapturedDate("2026.05.21")
        vm.setCapturedTime("14:30")
        assertTrue(vm.isValid())
    }

    @Test
    fun `setName clamps to 20 chars and comment to 50`() {
        val vm = SpotRegistrationViewModel(spotService)
        vm.setName("a".repeat(40))
        assertEquals(20, vm.name.value.length)
        vm.setComment("b".repeat(80))
        assertEquals(50, vm.comment.value.length)
    }

    @Test
    fun `submit fails fast when invalid`() = runTest(testDispatcher) {
        val vm = SpotRegistrationViewModel(spotService)
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.submission.value is LoadState.Failed)
    }

    @Test
    fun `submit success emits Loaded with registered spot`() = runTest(testDispatcher) {
        val captured = slot<SpotDraft>()
        coEvery { spotService.register(capture(captured)) } answers {
            Spot(
                id = "new-1",
                name = captured.captured.name,
                theme = captured.captured.theme,
                latitude = captured.captured.latitude,
                longitude = captured.captured.longitude,
                address = captured.captured.address,
            )
        }

        val vm = SpotRegistrationViewModel(spotService)
        vm.setName("Cafe")
        vm.setTheme(SpotTheme.BAR)
        vm.setAddress("Seoul")
        vm.setCoordinates(37.5, 127.0)
        vm.setCapturedDate("2026.05.21")
        vm.setCapturedTime("14:30")
        vm.submit()
        advanceUntilIdle()

        val state = vm.submission.value as LoadState.Loaded
        assertEquals("new-1", state.value.id)
        assertEquals(SpotTheme.BAR, state.value.theme)
        coVerify(exactly = 1) { spotService.register(any()) }
    }
}
