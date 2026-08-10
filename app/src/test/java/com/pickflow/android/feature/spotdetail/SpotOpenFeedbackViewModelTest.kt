package com.pickflow.android.feature.spotdetail

import com.pickflow.android.core.services.protocols.SpotReportService
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotOpenFeedbackViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var service: SpotReportService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        service = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit trims content and clears draft after success`() = runTest(dispatcher) {
        coEvery { service.report(41L, "잘못된 위치 정보입니다") } returns 1L
        val viewModel = SpotOpenFeedbackViewModel(service)
        viewModel.setDraft("  잘못된 위치 정보입니다  ")

        viewModel.submit(41L)
        advanceUntilIdle()

        coVerify(exactly = 1) { service.report(41L, "잘못된 위치 정보입니다") }
        assertEquals("", viewModel.draft.value)
        assertEquals("제보가 접수되었습니다.", viewModel.toast.value)
    }

    @Test
    fun `short content is ignored`() = runTest(dispatcher) {
        val viewModel = SpotOpenFeedbackViewModel(service)
        viewModel.setDraft("짧음")

        viewModel.submit(41L)
        advanceUntilIdle()

        coVerify(exactly = 0) { service.report(any(), any()) }
        assertEquals(null, viewModel.toast.value)
    }
}
