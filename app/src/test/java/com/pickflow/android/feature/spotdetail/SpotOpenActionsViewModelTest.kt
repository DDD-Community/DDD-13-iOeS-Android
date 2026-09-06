package com.pickflow.android.feature.spotdetail

import app.cash.turbine.test
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.MySpotUnpublishResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotOpenActionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mySpotService: MySpotService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mySpotService = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = SpotOpenActionsViewModel(mySpotService)

    @Test
    fun `requestOpen emits the new status and toasts`() = runTest(testDispatcher) {
        coEvery { mySpotService.requestOpen(41L) } returns
            MySpotTransitionResult(41L, MySpotStatus.PENDING)
        val viewModel = vm()

        viewModel.statusChanges.test {
            viewModel.requestOpen(41L)
            advanceUntilIdle()

            assertEquals(MySpotStatus.PENDING, awaitItem())
        }
        assertEquals("오픈 신청이 접수되었어요.", viewModel.toast.value)
        assertFalse(viewModel.isInFlight.value)
    }

    @Test
    fun `unpublish picks the message from previousStatus`() = runTest(testDispatcher) {
        // PENDING 에서 왔으면 "철회", PUBLISHED 에서 왔으면 "비공개 전환" 이다.
        coEvery { mySpotService.unpublish(41L) } returns
            MySpotUnpublishResult(41L, MySpotStatus.PENDING, MySpotStatus.DRAFT)
        val withdrawing = vm()
        withdrawing.unpublish(41L)
        advanceUntilIdle()
        assertEquals("오픈 신청을 철회했어요.", withdrawing.toast.value)

        coEvery { mySpotService.unpublish(42L) } returns
            MySpotUnpublishResult(42L, MySpotStatus.PUBLISHED, MySpotStatus.DRAFT)
        val unpublishing = vm()
        unpublishing.unpublish(42L)
        advanceUntilIdle()
        assertEquals("스팟을 비공개로 전환했어요.", unpublishing.toast.value)
    }

    @Test
    fun `a second tap while in flight is ignored`() = runTest(testDispatcher) {
        val response = CompletableDeferred<MySpotTransitionResult>()
        coEvery { mySpotService.requestOpen(41L) } coAnswers { response.await() }
        val viewModel = vm()

        viewModel.requestOpen(41L)
        runCurrent()
        assertTrue(viewModel.isInFlight.value)
        viewModel.requestOpen(41L)
        runCurrent()

        response.complete(MySpotTransitionResult(41L, MySpotStatus.PENDING))
        advanceUntilIdle()

        coVerify(exactly = 1) { mySpotService.requestOpen(41L) }
    }

    @Test
    fun `delete emits the id and failure falls back to the retry toast`() =
        runTest(testDispatcher) {
            coEvery { mySpotService.delete(41L) } returns Unit
            val viewModel = vm()

            viewModel.deleted.test {
                viewModel.delete(41L)
                advanceUntilIdle()
                assertEquals(41L, awaitItem())
            }

            coEvery { mySpotService.delete(9L) } throws RuntimeException("net")
            val failing = vm()
            failing.delete(9L)
            advanceUntilIdle()

            assertEquals("잠시 후 다시 시도해주세요.", failing.toast.value)
            assertFalse(failing.isInFlight.value)
        }
}
