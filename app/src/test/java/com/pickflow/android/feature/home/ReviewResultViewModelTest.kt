package com.pickflow.android.feature.home

import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.ReviewDecision
import com.pickflow.android.core.services.protocols.ReviewResult
import com.pickflow.android.core.services.protocols.ReviewResultService
import com.pickflow.android.core.services.protocols.ReviewResultStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: ReviewResultService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        service = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun result(
        resultId: Long,
        decision: ReviewDecision = ReviewDecision.APPROVED,
        occurredAt: String = "2026-08-06T10:00:00Z",
    ) = ReviewResult(
        resultId = resultId,
        spotId = resultId + 100L,
        decision = decision,
        occurredAt = occurredAt,
    )

    @Test
    fun `pending request keeps archive indicator without unseen result`() = runTest(testDispatcher) {
        val expected = ReviewResultStatus(pendingRequestCount = 1, unacknowledgedResults = emptyList())
        coEvery { service.status() } returns expected
        val viewModel = ReviewResultViewModel(service)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(LoadState.Loaded(expected), viewModel.status.value)
        assertTrue(viewModel.hasIndicator.value)
        assertEquals(null, viewModel.latestUnacknowledgedResult.value)
    }

    @Test
    fun `latest unseen result keeps archive indicator`() = runTest(testDispatcher) {
        val older = result(1L, occurredAt = "2026-08-06T09:00:00Z")
        val latest = result(
            2L,
            decision = ReviewDecision.REJECTED,
            occurredAt = "2026-08-06T11:00:00Z",
        )
        coEvery { service.status() } returns ReviewResultStatus(
            pendingRequestCount = 0,
            unacknowledgedResults = listOf(latest, older),
        )
        val viewModel = ReviewResultViewModel(service)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.hasIndicator.value)
        assertEquals(latest, viewModel.latestUnacknowledgedResult.value)
    }

    @Test
    fun `acknowledge removes only selected result and preserves remaining indicator`() =
        runTest(testDispatcher) {
            val first = result(1L)
            val second = result(2L, decision = ReviewDecision.REJECTED)
            coEvery { service.status() } returns ReviewResultStatus(0, listOf(first, second))
            coEvery { service.acknowledge(2L) } just Runs
            val viewModel = ReviewResultViewModel(service)
            viewModel.load()
            advanceUntilIdle()

            viewModel.acknowledge(2L)
            advanceUntilIdle()

            val loaded = viewModel.status.value as LoadState.Loaded
            assertEquals(listOf(1L), loaded.value.unacknowledgedResults.map { it.resultId })
            assertTrue(viewModel.hasIndicator.value)
            assertEquals(first, viewModel.latestUnacknowledgedResult.value)
            coVerify(exactly = 1) { service.acknowledge(2L) }
        }

    @Test
    fun `acknowledge failure leaves loaded screen state usable`() = runTest(testDispatcher) {
        val unseen = result(1L)
        val expected = ReviewResultStatus(0, listOf(unseen))
        val failure = RuntimeException("mark read failed")
        coEvery { service.status() } returns expected
        coEvery { service.acknowledge(1L) } throws failure
        val viewModel = ReviewResultViewModel(service)
        viewModel.load()
        advanceUntilIdle()

        viewModel.acknowledge(1L)
        advanceUntilIdle()

        assertEquals(LoadState.Loaded(expected), viewModel.status.value)
        assertTrue(viewModel.hasIndicator.value)
        assertEquals(unseen, viewModel.latestUnacknowledgedResult.value)
        assertSame(failure, viewModel.acknowledgementError.value)
    }

    @Test
    fun `published modal acknowledgement failure does not replace loaded status`() =
        runTest(testDispatcher) {
            val approved = result(1L)
            val expected = ReviewResultStatus(0, listOf(approved))
            val failure = RuntimeException("modal mark failed")
            coEvery { service.status() } returns expected
            coEvery { service.acknowledgePublishedModal(1L) } throws failure
            val viewModel = ReviewResultViewModel(service)
            viewModel.load()
            advanceUntilIdle()

            viewModel.acknowledgePublishedModal(1L)
            advanceUntilIdle()

            assertEquals(LoadState.Loaded(expected), viewModel.status.value)
            assertTrue(viewModel.hasIndicator.value)
            assertFalse(approved.publishedModalAcknowledged)
            assertSame(failure, viewModel.acknowledgementError.value)
        }

    @Test
    fun `status failure emits Failed without stale indicator`() = runTest(testDispatcher) {
        val failure = RuntimeException("status failed")
        coEvery { service.status() } throws failure
        val viewModel = ReviewResultViewModel(service)

        viewModel.load()
        advanceUntilIdle()

        assertTrue(viewModel.status.value is LoadState.Failed)
        assertFalse(viewModel.hasIndicator.value)
        assertSame(failure, (viewModel.status.value as LoadState.Failed).error)
    }
}
