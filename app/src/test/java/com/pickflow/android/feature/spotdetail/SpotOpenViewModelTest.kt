package com.pickflow.android.feature.spotdetail

import app.cash.turbine.test
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotTransitionConflictException
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import com.pickflow.android.core.services.protocols.SpotSource
import com.pickflow.android.core.services.protocols.SpotTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotOpenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: MySpotService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        service = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun detail(status: MySpotStatus = MySpotStatus.DRAFT) = MySpotDetail(
        id = 41L,
        name = "노을 공원",
        theme = SpotTheme.SUNSET,
        imageUrl = null,
        latitude = 37.0,
        longitude = 127.0,
        address = "서울",
        capturedDate = "2026-08-06",
        capturedTime = "19:20",
        comment = "노을이 예뻐요",
        status = status,
        rejectionReason = null,
        recommendationCount = 7L,
        isRecommended = false,
        source = SpotSource.User,
        updatedAt = "2026-08-06T10:00:00Z",
    )

    private fun result(status: MySpotStatus) = MySpotTransitionResult(
        spotId = 41L,
        status = status,
        updatedAt = "2026-08-06T10:01:00Z",
    )

    @Test
    fun `load returns Loaded detail`() = runTest(testDispatcher) {
        val expected = detail()
        coEvery { service.detail(41L) } returns expected
        val viewModel = SpotOpenViewModel(service)

        assertEquals(LoadState.Idle, viewModel.detail.value)
        viewModel.load(41L)
        advanceUntilIdle()

        assertEquals(LoadState.Loaded(expected), viewModel.detail.value)
    }

    @Test
    fun `load emits Failed when detail request fails`() = runTest(testDispatcher) {
        val failure = RuntimeException("detail failed")
        coEvery { service.detail(41L) } throws failure
        val viewModel = SpotOpenViewModel(service)

        viewModel.load(41L)
        advanceUntilIdle()

        val state = viewModel.detail.value
        assertTrue(state is LoadState.Failed)
        assertSame(failure, (state as LoadState.Failed).error)
    }

    @Test
    fun `request open disables duplicate action and applies final transition`() = runTest(testDispatcher) {
        val response = CompletableDeferred<MySpotTransitionResult>()
        coEvery { service.detail(41L) } returns detail()
        coEvery { service.requestOpen(41L) } coAnswers { response.await() }
        val viewModel = SpotOpenViewModel(service)
        viewModel.load(41L)
        advanceUntilIdle()

        viewModel.requestOpen()
        viewModel.requestOpen()
        runCurrent()

        assertTrue(viewModel.isTransitionInFlight.value)
        coVerify(exactly = 1) { service.requestOpen(41L) }

        response.complete(result(MySpotStatus.PENDING))
        advanceUntilIdle()

        assertFalse(viewModel.isTransitionInFlight.value)
        assertEquals(MySpotStatus.PENDING, viewModel.loadedDetail().status)
        assertEquals("2026-08-06T10:01:00Z", viewModel.loadedDetail().updatedAt)
        assertEquals("오픈 신청이 접수되었어요", viewModel.toast.value)
    }

    @Test
    fun `each state action delegates to matching service transition`() = runTest(testDispatcher) {
        coEvery { service.detail(41L) } returns detail(MySpotStatus.REJECTED)
        coEvery { service.withdrawRejection(41L) } returns result(MySpotStatus.DRAFT)
        coEvery { service.withdrawRequest(41L) } returns result(MySpotStatus.DRAFT)
        coEvery { service.cancelOpen(41L) } returns result(MySpotStatus.DRAFT)
        val viewModel = SpotOpenViewModel(service)
        viewModel.load(41L)
        advanceUntilIdle()

        viewModel.withdrawRejection()
        advanceUntilIdle()
        viewModel.withdrawRequest()
        advanceUntilIdle()
        viewModel.cancelOpen()
        advanceUntilIdle()

        coVerify(exactly = 1) { service.withdrawRejection(41L) }
        coVerify(exactly = 1) { service.withdrawRequest(41L) }
        coVerify(exactly = 1) { service.cancelOpen(41L) }
        assertEquals(MySpotStatus.DRAFT, viewModel.loadedDetail().status)
    }

    @Test
    fun `transition failure keeps previous detail and shows retry toast`() = runTest(testDispatcher) {
        val original = detail(MySpotStatus.PUBLISHED)
        coEvery { service.detail(41L) } returns original
        coEvery { service.cancelOpen(41L) } throws RuntimeException("network")
        val viewModel = SpotOpenViewModel(service)
        viewModel.load(41L)
        advanceUntilIdle()

        viewModel.cancelOpen()
        advanceUntilIdle()

        assertEquals(LoadState.Loaded(original), viewModel.detail.value)
        assertFalse(viewModel.isTransitionInFlight.value)
        assertEquals("실패했어요, 다시 시도해주세요", viewModel.toast.value)
    }

    @Test
    fun `withdraw conflict shows processed toast and force reloads latest detail`() = runTest(testDispatcher) {
        val pending = detail(MySpotStatus.PENDING)
        val published = detail(MySpotStatus.PUBLISHED)
        coEvery { service.detail(41L) } returnsMany listOf(pending, published)
        coEvery { service.withdrawRequest(41L) } throws MySpotTransitionConflictException(
            41L,
            MySpotStatus.PUBLISHED,
        )
        val viewModel = SpotOpenViewModel(service)
        viewModel.load(41L)
        advanceUntilIdle()

        viewModel.withdrawRequest()
        advanceUntilIdle()

        assertEquals(LoadState.Loaded(published), viewModel.detail.value)
        assertEquals("이미 처리된 신청이에요", viewModel.toast.value)
        assertFalse(viewModel.isTransitionInFlight.value)
        coVerify(exactly = 2) { service.detail(41L) }
    }

    @Test
    fun `delete emits one completion event and blocks duplicate request`() = runTest(testDispatcher) {
        val response = CompletableDeferred<Unit>()
        coEvery { service.detail(41L) } returns detail()
        coEvery { service.delete(41L) } coAnswers { response.await() }
        val viewModel = SpotOpenViewModel(service)
        viewModel.load(41L)
        advanceUntilIdle()

        viewModel.deletedSpotIds.test {
            viewModel.delete()
            viewModel.delete()
            runCurrent()
            coVerify(exactly = 1) { service.delete(41L) }

            response.complete(Unit)
            assertEquals(41L, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete failure keeps detail and does not emit completion event`() = runTest(testDispatcher) {
        val original = detail()
        coEvery { service.detail(41L) } returns original
        coEvery { service.delete(41L) } throws RuntimeException("network")
        val viewModel = SpotOpenViewModel(service)
        viewModel.load(41L)
        advanceUntilIdle()

        viewModel.deletedSpotIds.test {
            viewModel.delete()
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(LoadState.Loaded(original), viewModel.detail.value)
            assertEquals("실패했어요, 다시 시도해주세요", viewModel.toast.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun SpotOpenViewModel.loadedDetail(): MySpotDetail =
        (detail.value as LoadState.Loaded<MySpotDetail>).value
}
