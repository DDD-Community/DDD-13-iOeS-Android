package com.pickflow.android.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 페이지 전환 애니메이션이 끝난 직후 토스트가 등장하도록 주는 지연(ms). iOS `toastPresentDelay`. */
private const val TOAST_PRESENT_DELAY_MS = 250L

/** Step 1 진입 시 띄우는 토스트 문구. iOS와 동일. */
private const val SPOT_REGISTERED_TOAST = "나만의 스팟이 등록되었어요!"

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completionStore: OnboardingCompletionStore,
) : ViewModel() {

    private val _pageIndex = MutableStateFlow(0)
    val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    /** Step 1 이미지 뒤에서 올라오는 토스트 문구. null이면 비표시. iOS `toast`. */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val pages: List<OnboardingPage> = OnboardingPage.entries

    fun next() {
        val previous = _pageIndex.value
        val nextIndex = previous + 1
        if (nextIndex >= pages.size) {
            finish()
        } else {
            _pageIndex.value = nextIndex
            handlePageTransition(previous, nextIndex)
        }
    }

    fun previous() {
        if (_pageIndex.value > 0) _pageIndex.value -= 1
    }

    /** 스와이프 페이저가 보고하는 현재 페이지를 반영한다(범위 밖 값은 clamp). */
    fun setPage(index: Int) {
        val previous = _pageIndex.value
        val clamped = index.coerceIn(0, pages.lastIndex)
        _pageIndex.value = clamped
        handlePageTransition(previous, clamped)
    }

    fun finish() {
        viewModelScope.launch {
            completionStore.markCompleted()
            _completed.value = true
        }
    }

    /**
     * 페이지 전환에 따라 Step 1 토스트를 띄우거나 내린다. iOS `handlePageTransition` 1:1.
     * 0번에서 벗어나거나 1번으로 진입할 때 토스트를 예약하고, 그 외 전환에서는 즉시 내린다.
     */
    private fun handlePageTransition(previous: Int, next: Int) {
        if (previous == 0 || next == 1) {
            presentToast(SPOT_REGISTERED_TOAST)
        } else {
            dismissToast()
        }
    }

    /** 페이지 전환 애니메이션 종료 후 토스트가 뜨도록 지연했다가, 여전히 Step 1일 때만 표시. */
    private fun presentToast(message: String) {
        viewModelScope.launch {
            delay(TOAST_PRESENT_DELAY_MS)
            if (_pageIndex.value == 1) _toast.value = message
        }
    }

    private fun dismissToast() {
        _toast.value = null
    }
}

enum class OnboardingPage(val titleKey: String) {
    INTRO("onboarding.intro"),
    EXPLORE("onboarding.explore"),
    REGISTER("onboarding.register"),
    READY("onboarding.ready"),
}
