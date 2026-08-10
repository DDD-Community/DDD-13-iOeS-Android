package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.RecommendationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpotRecommendationUiState(
    val spotId: Long? = null,
    val recommendationCount: Long = 0L,
    val isRecommended: Boolean = false,
    val isInFlight: Boolean = false,
)

@HiltViewModel
class SpotRecommendationViewModel @Inject constructor(
    private val authService: AuthService,
    private val recommendationService: RecommendationService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpotRecommendationUiState())
    val uiState: StateFlow<SpotRecommendationUiState> = _uiState.asStateFlow()

    private val _isLoginRequired = MutableStateFlow(false)
    val isLoginRequired: StateFlow<Boolean> = _isLoginRequired.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun initialize(
        spotId: Long,
        recommendationCount: Long,
        isRecommended: Boolean,
    ) {
        if (_uiState.value.isInFlight) return
        _uiState.value = SpotRecommendationUiState(
            spotId = spotId,
            recommendationCount = recommendationCount.coerceAtLeast(0L),
            isRecommended = isRecommended,
        )
    }

    fun toggleRecommendation() {
        val previous = _uiState.value
        val spotId = previous.spotId ?: return
        if (previous.isInFlight) return

        // Lock synchronously so two taps before the coroutine starts still produce one request.
        _uiState.value = previous.copy(isInFlight = true)
        viewModelScope.launch {
            try {
                if (!authService.isLoggedIn()) {
                    _uiState.value = previous
                    _isLoginRequired.value = true
                    return@launch
                }

                val optimistic = previous.copy(
                    recommendationCount = if (previous.isRecommended) {
                        (previous.recommendationCount - 1L).coerceAtLeast(0L)
                    } else {
                        previous.recommendationCount + 1L
                    },
                    isRecommended = !previous.isRecommended,
                    isInFlight = true,
                )
                _uiState.value = optimistic

                val result = if (previous.isRecommended) {
                    recommendationService.cancel(spotId)
                } else {
                    recommendationService.recommend(spotId)
                }
                _uiState.value = optimistic.copy(
                    recommendationCount = result.recommendationCount.coerceAtLeast(0L),
                    isRecommended = result.isRecommended,
                    isInFlight = false,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.value = previous
                _toast.value = RETRY_TOAST
            }
        }
    }

    fun dismissLoginRequired() {
        _isLoginRequired.value = false
    }

    fun consumeToast() {
        _toast.value = null
    }

    private companion object {
        const val RETRY_TOAST = "잠시 후 다시 시도해주세요"
    }
}
