package com.pickflow.android.feature.spotdetail

import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.RecommendationResult
import com.pickflow.android.core.services.protocols.RecommendationService
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotRecommendationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authService: AuthService
    private lateinit var recommendationService: RecommendationService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authService = mockk()
        recommendationService = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SpotRecommendationViewModel(authService, recommendationService).apply {
        initialize(spotId = 41L, recommendationCount = 7L, isRecommended = false)
    }

    @Test
    fun `logged out tap requests login without optimistic update or service call`() =
        runTest(testDispatcher) {
            coEvery { authService.isLoggedIn() } returns false
            val viewModel = viewModel()

            viewModel.toggleRecommendation()
            advanceUntilIdle()

            assertTrue(viewModel.isLoginRequired.value)
            assertEquals(7L, viewModel.uiState.value.recommendationCount)
            assertFalse(viewModel.uiState.value.isRecommended)
            assertFalse(viewModel.uiState.value.isInFlight)
            coVerify(exactly = 0) { recommendationService.recommend(any()) }
            coVerify(exactly = 0) { recommendationService.cancel(any()) }
        }

    @Test
    fun `recommend is optimistic blocks duplicate taps then uses server final value`() =
        runTest(testDispatcher) {
            val response = CompletableDeferred<RecommendationResult>()
            coEvery { authService.isLoggedIn() } returns true
            coEvery { recommendationService.recommend(41L) } coAnswers { response.await() }
            val viewModel = viewModel()

            viewModel.toggleRecommendation()
            viewModel.toggleRecommendation()
            runCurrent()

            assertTrue(viewModel.uiState.value.isRecommended)
            assertEquals(8L, viewModel.uiState.value.recommendationCount)
            assertTrue(viewModel.uiState.value.isInFlight)
            coVerify(exactly = 1) { recommendationService.recommend(41L) }

            response.complete(RecommendationResult(41L, 12L, false))
            advanceUntilIdle()

            assertEquals(12L, viewModel.uiState.value.recommendationCount)
            assertFalse(viewModel.uiState.value.isRecommended)
            assertFalse(viewModel.uiState.value.isInFlight)
        }

    @Test
    fun `recommended tap optimistically cancels and uses server final value`() =
        runTest(testDispatcher) {
            coEvery { authService.isLoggedIn() } returns true
            coEvery { recommendationService.cancel(41L) } returns
                RecommendationResult(41L, 5L, false)
            val viewModel = SpotRecommendationViewModel(authService, recommendationService).apply {
                initialize(spotId = 41L, recommendationCount = 7L, isRecommended = true)
            }

            viewModel.toggleRecommendation()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRecommended)
            assertEquals(5L, viewModel.uiState.value.recommendationCount)
            assertFalse(viewModel.uiState.value.isInFlight)
            coVerify(exactly = 1) { recommendationService.cancel(41L) }
        }

    @Test
    fun `recommend failure rolls back button and count and shows retry toast`() =
        runTest(testDispatcher) {
            coEvery { authService.isLoggedIn() } returns true
            coEvery { recommendationService.recommend(41L) } throws RuntimeException("network")
            val viewModel = viewModel()

            viewModel.toggleRecommendation()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRecommended)
            assertEquals(7L, viewModel.uiState.value.recommendationCount)
            assertFalse(viewModel.uiState.value.isInFlight)
            assertEquals("잠시 후 다시 시도해주세요", viewModel.toast.value)
        }
}
